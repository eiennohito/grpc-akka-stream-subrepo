package org.eiennohito.grpc.stream.impl

import akka.actor.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{ActorMaterializer, Attributes, Inlet, Outlet, SinkShape, SourceShape}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.{Context, Metadata, ServerCall, ServerCallHandler, Status}
import org.eiennohito.grpc.stream.GrpcStreaming
import org.eiennohito.grpc.stream.server.CallMetadata

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
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
  extends GraphStageWithMaterializedValue[SourceShape[Req], Future[ActorRef]] {
  val out = Outlet[Req]("GrpcServer.request")

  override val shape: SourceShape[Req] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val promise = Promise[ActorRef]

    val logic = new GraphStageLogic(shape) with OutHandler {
      private val buffer = new mutable.Queue[Req]()
      private val bufSize = inheritedAttributes.get(new Attributes.InputBuffer(8, 16))
      private var inFlight = 0

      private def request() = {
        if (inFlight < bufSize.initial && buffer.size < bufSize.initial) {
          val toRequest = bufSize.max - inFlight
          call.request(toRequest)
          inFlight += toRequest
        }
      }

      override def onPull(): Unit = {
        if (buffer.nonEmpty) {
          push(out, buffer.dequeue())
        }
        request()
      }

      private def handleInput(o: Req): Unit = {
        if (isAvailable(out) && buffer.isEmpty) {
          push(out, o)
        } else buffer += o
      }

      override def preStart() = {
        super.preStart()
        val reqTag = implicitly[ClassTag[Req]]
        val actor = getStageActor {
          case (_, GrpcMessages.Complete) => completeStage()
          case (_, reqTag(msg)) => handleInput(msg)
        }
        promise.success(actor.ref)
      }
      request()
    }
    (logic, promise.future)
  }
}

class GrpcServerSink[Resp](call: ServerCall[_, Resp]) extends GraphStageWithMaterializedValue[SinkShape[Resp], Future[ActorRef]] {
  val in = Inlet[Resp]("GrpcServer.response")

  override val shape = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[ActorRef]) = {
    val promise = Promise[ActorRef]()
    val logic = new GraphStageLogic(shape) with InHandler {
      override def onPush(): Unit = {
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
      }

      override def onUpstreamFinish() = {
        call.close(Status.OK, null)
      }

      override def onUpstreamFailure(ex: Throwable) = {
        call.close(Status.fromThrowable(ex), Status.trailersFromThrowable(ex))
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

  private [this] val inputPromise = Promise[ActorRef]()
  private [this] val outputPromise = Promise[ActorRef]()

  {
    val ctx = Context.current()
    val meta = CallMetadata(ctx, headers)
    val logic = handler.apply(meta)

    logic.onComplete {
      case Success(f) =>
        val src = Source.fromGraph(new GrpcServerSrc[Req](call))
        val sink = Sink.fromGraph(new GrpcServerSink[Resp](call))
        val (f1, f2) = f.runWith(src, sink)
        inputPromise.completeWith(f1)
        outputPromise.completeWith(f2)
      case Failure(e) =>
        logger.error("could not create logic", e)
        call.close(Status.UNKNOWN.withCause(e), null)
        inputPromise.failure(e)
        outputPromise.failure(e)
    }
  }

  private[this] val inputRef = inputPromise.future
  private[this] val outputRef = outputPromise.future

  override def onMessage(message: Req) = {
    inputRef.map(_ ! message)
  }

  override def onCancel() = {
    inputRef.map(_ ! GrpcMessages.Complete)
    outputRef.map(_ ! GrpcMessages.Complete)
  }

  override def onComplete() = {
    outputRef.map(_ ! GrpcMessages.Complete)
  }

  override def onReady() = {
    outputRef.map(_ ! GrpcMessages.Ready)
  }

  override def onHalfClose() = {
    inputRef.map(_ ! GrpcMessages.Complete)
  }
}
