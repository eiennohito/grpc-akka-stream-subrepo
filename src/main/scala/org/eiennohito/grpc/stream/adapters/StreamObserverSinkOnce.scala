package org.eiennohito.grpc.stream.adapters

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import io.grpc.stub.StreamObserver

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class StreamObserverSinkOnce[T](so: StreamObserver[T]) extends GraphStage[SinkShape[T]] {
  val in: Inlet[T] = Inlet("StreamObserverOnce")
  override val shape = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes) = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush() = {
          val elem = grab(in)
          so.onNext(elem)
          completeStage()
          so.onCompleted()
        }

        override def onUpstreamFinish() = {
          so.onCompleted()
        }

        override def onUpstreamFailure(ex: Throwable) = {
          so.onError(ex)
        }
      })
    }
  }
}
