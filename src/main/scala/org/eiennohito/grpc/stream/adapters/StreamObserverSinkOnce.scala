/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eiennohito.grpc.stream.adapters

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.Status
import io.grpc.stub.StreamObserver

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class StreamObserverSinkOnce[T](so: StreamObserver[T]) extends GraphStage[SinkShape[T]] with StrictLogging {
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
          logger.error("error in stream observer once", ex)
          so.onError(Status.INTERNAL.withCause(ex).asException())
        }
      })

      override def preStart() = {
        super.preStart()
        pull(in)
      }
    }
  }
}
