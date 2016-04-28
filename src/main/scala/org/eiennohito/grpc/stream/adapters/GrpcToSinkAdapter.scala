package org.eiennohito.grpc.stream.adapters

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import io.grpc.stub.StreamObserver

/**
  * @author eiennohito
  * @since 2016/04/28
  */
trait ReadyHandler {
  def onReady(): Unit
  def onCancel(): Unit
}

trait ReadyInput {
  def isReady: Boolean
}

class GrpcToSinkAdapter[T](data: StreamObserver[T], rdy: ReadyInput)
  extends GraphStageWithMaterializedValue[SinkShape[T], ReadyHandler] {

  private val in = Inlet[T]("Grpc.Out")
  override val shape = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {

    var hndler: ReadyHandler = null

    val logic = new GraphStageLogic(shape) { logic =>

      hndler = new ReadyHandler {
        private val readyCall = getAsyncCallback { _: Unit => signalReady() }
        private val cancelCall = getAsyncCallback { _: Unit => signalCancel() }
        override def onReady() = readyCall.invoke()
        override def onCancel() = cancelCall.invoke()
      }

      if (rdy.isReady) {
        pull(in)
      }

      private def signalReady(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
        }
      }

      private def signalCancel(): Unit = {
        cancel(in)
        logic.completeStage()
      }

      setHandler(in, new InHandler {
        override def onPush() = {
          data.onNext(grab(in))
          if (rdy.isReady) {
            pull(in)
          }
        }
        override def onUpstreamFinish() = data.onCompleted()
        override def onUpstreamFailure(ex: Throwable) = data.onError(ex)
      })
    }

    (logic, hndler)
  }
}
