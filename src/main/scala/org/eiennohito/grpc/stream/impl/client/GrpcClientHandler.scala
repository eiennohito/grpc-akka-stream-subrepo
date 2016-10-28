package org.eiennohito.grpc.stream.impl.client

import java.util.UUID

import akka.Done
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import io.grpc._
import org.eiennohito.grpc.stream.client.GrpcCallStatus
import org.eiennohito.grpc.stream.impl.{GrpcMessages, ScalaMetadata}

import scala.collection.mutable
import scala.concurrent.Promise

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class GrpcClientHandler[Req, Resp](
  chan: Channel,
  md: MethodDescriptor[Req, Resp],
  opts: CallOptions
) extends GraphStageWithMaterializedValue[FlowShape[Req, Resp], GrpcCallStatus] {
  val in = Inlet[Req]("GrpcClient.request")
  val out = Outlet[Resp]("GrpcClient.response")
  val shape = FlowShape(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, GrpcCallStatus) = {

    val hdrs = Promise[Metadata]()
    val trls = Promise[Metadata]()
    val cmpl = Promise[Done]()

    val myid = inheritedAttributes.get[ScalaMetadata.InitialRequestId].map(_.id).getOrElse(UUID.randomUUID())

    val logic = new GraphStageLogic(shape) with InHandler with OutHandler {
      setHandlers(in, out, this)
      private val call = chan.newCall(md, opts)
      private val params = inheritedAttributes.get(Attributes.InputBuffer(8, 16))
      private val buffer = new mutable.Queue[Resp]()
      private var inFlight = 0

      private def request(): Unit = {
        if (inFlight < params.initial && buffer.size < params.initial) {
          val toRequest = params.max - inFlight
          call.request(toRequest)
          inFlight += toRequest
        }
      }

      override def onPush(): Unit = {
        val msg = grab(in)
        call.sendMessage(msg)
        if (isAvailable(in)) {
          pull(in)
        }
      }

      override def onPull(): Unit = {
        if (buffer.nonEmpty) {
          push(out, buffer.dequeue())
        }
        request()
      }

      override def onUpstreamFinish() = {
        call.halfClose()
      }

      override def onUpstreamFailure(ex: Throwable) = {
        call.cancel("cancelled because of exception", ex)
        super.onUpstreamFailure(ex)
        hdrs.tryFailure(ex)
        trls.tryFailure(ex)
        cmpl.failure(ex)
      }

      override def onDownstreamFinish() = {
        call.cancel("downstream finished", null)
        cmpl.success(Done)
        super.onDownstreamFinish()
      }

      private def handleResp(resp: Resp) = {
        inFlight -= 1
        if (isAvailable(out) && buffer.isEmpty) {
          push(out, resp)
        } else buffer += resp
        request()
      }

      private def handleClose(t: Status, m: Metadata) = {
        trls.success(m)
        if (t.isOk) {
          cmpl.success(Done)
          completeStage()
        } else {
          val ex = ScalaMetadata.get(m, ScalaMetadata.ScalaException) match {
            case None => t.asException()
            case Some(t2) =>
              val e = t.asException()
              e.addSuppressed(t2)
              e
          }
          cmpl.failure(ex)
          failStage(ex)
        }
      }

      override def preStart() = {
        val actor = getStageActor {
          case (_, GrpcMessages.Ready) => if (!hasBeenPulled(in) && !isClosed(in)) { pull(in) }
          case (_, GrpcMessages.Close(t, m)) => handleClose(t, m)
          case (_, GrpcMessages.Headers(h)) => hdrs.success(h)
          case (_, x) => handleResp(x.asInstanceOf[Resp])
        }

        call.start(new ClientCall.Listener[Resp] {
          override def onMessage(message: Resp) = actor.ref ! message
          override def onClose(status: Status, trailers: Metadata) = actor.ref ! GrpcMessages.Close(status, trailers)
          override def onHeaders(headers: Metadata) = actor.ref ! GrpcMessages.Headers(headers)
          override def onReady() = actor.ref ! GrpcMessages.Ready
        }, ScalaMetadata.make(myid))
        request()
        pull(in)
      }
    }

    (logic, GrpcCallStatus(myid, hdrs.future, trls.future, cmpl.future))
  }
}

