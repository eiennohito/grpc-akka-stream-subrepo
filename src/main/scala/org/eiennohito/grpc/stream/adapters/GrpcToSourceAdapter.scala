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

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.actor.RequestStrategy
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}
import io.grpc.stub.StreamObserver
import org.reactivestreams.Subscription


trait Requester extends Subscription {
  def request(number: Int): Unit = this.request(number.toLong)
  def cancel(): Unit
}

class GrpcToSourceAdapter[T](req: Subscription, rs: RequestStrategy, private var inFlight: Int)
  extends GraphStageWithMaterializedValue[SourceShape[T], StreamObserver[T]] {

  private val out = Outlet[T]("Grpc.In")
  override val shape = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    var observer: StreamObserver[T] = null

    val logic = new GraphStageLogic(shape) { logic =>
      val queue = new scala.collection.mutable.Queue[T]()

      observer = new StreamObserver[T] {
        private val nextAction = getAsyncCallback { x: T => supply(x) }
        private val errorAction = getAsyncCallback { x: Throwable => logic.failStage(x) }
        private val completeAction = getAsyncCallback { _: Unit => logic.completeStage() }

        override def onError(t: Throwable) = errorAction.invoke(t)
        override def onCompleted() = completeAction.invoke(())
        override def onNext(value: T) = nextAction.invoke(value)
      }

      private def supply(x: T) = {
        if (isAvailable(out)) {
          push(out, x)
        } else {
          queue += x
        }
        inFlight -= 1
        requestDemand()
      }

      private def requestDemand() = {
        val demand = rs.requestDemand(inFlight)
        if (demand > 0) {
          req.request(demand)
        }
      }

      setHandler(out, new OutHandler {
        override def onPull() = {
          if (queue.nonEmpty) {
            push(out, queue.dequeue())
            requestDemand()
          }
        }

        override def onDownstreamFinish() = {
          req.cancel()
          super.onDownstreamFinish()
        }
      })
    }

    (logic, observer)
  }
}
