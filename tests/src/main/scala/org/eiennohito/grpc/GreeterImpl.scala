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

package org.eiennohito.grpc

import scala.collection.mutable

import io.grpc.stub.StreamObserver
import org.eiennohito.grpc.GreeterGrpc.Greeter
import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class GreeterImpl extends Greeter {
  override def sayHello(request: HelloRequest) = {
    Future.successful(HelloReply("Hi, " + request.name))
  }

  override def sayHelloSvrStream(request: HelloRequestStream, responseObserver: StreamObserver[HelloReply]) = {
    for (i <- 0 until request.number) {
      responseObserver.onNext(HelloReply(s"Hi, ${request.name}, #$i"))
    }
    responseObserver.onCompleted()
  }

  def sayHelloClientStream(responseObserver: StreamObserver[HelloStreamReply]): StreamObserver[HelloRequest] =
    new StreamObserver[HelloRequest] {
      val arr = mutable.ArrayBuffer.empty[HelloRequest]
      def onError(t: Throwable): Unit = responseObserver.onError(t)
      def onNext(value: HelloRequest): Unit = arr += value
      def onCompleted(): Unit = {
        responseObserver.onNext(HelloStreamReply(arr.size, "Hi, " + arr.mkString(", ")))
        responseObserver.onCompleted()
      }
    }
}
