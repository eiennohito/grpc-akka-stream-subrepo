package org.eiennohito.grpc.stream.impl

import akka.actor.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{AsyncCallback, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{ActorMaterializer, Attributes, Inlet, Outlet, SinkShape, SourceShape}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.{Context, Metadata, ServerCall, ServerCallHandler, Status}
import org.eiennohito.grpc.stream.GrpcStreaming
import org.eiennohito.grpc.stream.server.CallMetadata

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class ServerAkkaStreamHandler[Req: ClassTag, Resp](handler: GrpcStreaming.ServerHandler[Req, Resp, _])(implicit ec: ExecutionContext, amat: ActorMaterializer)
  extends ServerCallHandler[Req, Resp] {
  override def startCall(call: ServerCall[Req, Resp], headers: Metadata) = {
    new ServerAkkaHandler(call, headers, handler)
  }
}

class GrpcServerSrc[Req: ClassTag](call: ServerCall[Req, _])
  extends GraphStageWithMaterializedValue[SourceShape[Req], Future[AsyncCallback[Any]]] with StrictLogging {
  val out = Outlet[Req]("GrpcServer.request")

  override val shape: SourceShape[Req] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val promise = Promise[AsyncCallback[Any]]

    val logic = new GraphStageLogic(shape) with OutHandler {
      setHandler(out, this)

      private val buffer = new mutable.Queue[Req]()
      private val bufSize = inheritedAttributes.get(new Attributes.InputBuffer(8, 16))
      private var inFlight = 0

      private def request() = {
        if (inFlight < bufSize.initial && buffer.size < bufSize.initial) {
          val toRequest = bufSize.max - inFlight
          call.request(toRequest)
          inFlight += toRequest
          //logger.debug(s"request $toRequest -> $inFlight")
        }
        if (buffer.isEmpty && inFlight > bufSize.max) {
          completeStage()
        }
      }

      override def onPull(): Unit = {
        if (buffer.nonEmpty) {
          push(out, buffer.dequeue())
          //logger.debug("pushed")
        }
        request()
      }

      private def handleInput(o: Req): Unit = {
        //logger.debug("input")
        inFlight -= 1
        if (isAvailable(out) && buffer.isEmpty) {
          push(out, o)
        } else buffer += o
        request()
      }

      override def preStart() = {
        super.preStart()
        val reqTag = implicitly[ClassTag[Req]]
        val cb = getAsyncCallback[Any] {
          case GrpcMessages.Complete => completeStage()
          case GrpcMessages.StopRequests =>
            inFlight = Int.MaxValue
            //logger.debug("stopreqs")
            request()
          case reqTag(msg) => handleInput(msg)
        }
        promise.success(cb)
      }
      request()
    }
    (logic, promise.future)
  }
}

class GrpcServerSink[Resp](call: ServerCall[_, Resp])
  extends GraphStageWithMaterializedValue[SinkShape[Resp], Future[ActorRef]] with StrictLogging {
  val in = Inlet[Resp]("GrpcServer.response")

  override val shape = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[ActorRef]) = {
    val promise = Promise[ActorRef]()
    val logic = new GraphStageLogic(shape) with InHandler {
      setHandler(in, this)

      override def onPush(): Unit = {
        //logger.debug("-> pushed")
        call.sendMessage(grab(in))
        if (call.isReady) {
          pull(in)
        }
      }

      override def preStart() = {
        val actor = getStageActor {
          case (_, GrpcMessages.Complete) => completeStage()
          case (_, GrpcMessages.Ready) => if (!hasBeenPulled(in)) pull(in)
        }
        promise.success(actor.ref)
        pull(in)
        //logger.debug("started")
      }

      override def onUpstreamFinish() = {
        //logger.debug("finished")
        call.close(Status.OK, new Metadata())
        super.onUpstreamFinish()
      }

      override def onUpstreamFailure(ex: Throwable) = {
        call.close(Status.fromThrowable(ex), ScalaMetadata.forException(ex))
        super.onUpstreamFinish()
      }
    }
    (logic, promise.future)
  }
}

class ServerAkkaHandler[Req: ClassTag, Resp](
  call: ServerCall[Req, Resp],
  headers: Metadata,
  handler: GrpcStreaming.ServerHandler[Req, Resp, _])(
  implicit ec: ExecutionContext,
  amat: ActorMaterializer
) extends ServerCall.Listener[Req] with StrictLogging {

  private [this] val inputPromise = Promise[AsyncCallback[Any]]()
  private [this] val outputPromise = Promise[ActorRef]()

  {
    val ctx = Context.current()
    val meta = CallMetadata(ctx, headers)
    val logic = handler.apply(meta)

    logic.onComplete {
      case Success(f) =>
        val src = Source.fromGraph(new GrpcServerSrc[Req](call))
        val sink = Sink.fromGraph(new GrpcServerSink[Resp](call))

        val baseId = ScalaMetadata.get(headers, ScalaMetadata.ReqId)

        val flow = baseId match {
          case None => f
          case Some(id) => f.addAttributes(Attributes(ScalaMetadata.InitialRequestId(id)))
        }

        call.sendHeaders(new Metadata())
        val (f1, f2) = flow.runWith(src, sink)
        inputPromise.completeWith(f1)
        outputPromise.completeWith(f2)
      case Failure(e) =>
        logger.error("could not create logic", e)
        call.close(Status.UNKNOWN.withCause(e), ScalaMetadata.forException(e))
        inputPromise.failure(e)
        outputPromise.failure(e)
    }
  }

  //TODO fix after https://github.com/akka/akka/issues/21741 or https://github.com/akka/akka/issues/20503
  import scala.concurrent.duration._
  private[this] lazy val inputRef: AsyncCallback[Any] = Await.result(inputPromise.future, 1.second)
  private[this] lazy val outputRef = Await.result(outputPromise.future, 1.second)

  override def onMessage(message: Req) = {
    inputRef.invoke(message)
    //logger.debug("msg")
  }

  override def onCancel() = {
    inputRef.invoke(GrpcMessages.Complete)
    outputRef ! GrpcMessages.Complete
    //logger.debug("cancel")
  }

  override def onComplete() = {
    inputRef.invoke(GrpcMessages.Complete)
    //logger.debug("cmpl")
  }

  override def onReady() = {
    outputRef ! GrpcMessages.Ready
    //logger.debug("rdy")
  }

  override def onHalfClose() = {
    inputRef.invoke(GrpcMessages.StopRequests)
    //logger.debug("half")
  }
}
