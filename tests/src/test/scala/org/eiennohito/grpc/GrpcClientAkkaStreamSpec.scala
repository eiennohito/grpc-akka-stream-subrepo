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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.TestKit
import org.eiennohito.grpc.stream.client.InboundStream

import scala.concurrent.Await

import scala.concurrent.duration._
/**
  * @author eiennohito
  * @since 2016/04/29
  */
class GrpcClientAkkaStreamSpec extends TestKit(ActorSystem()) with GrpcServerClientSpec {
  override def init = _.addService(GreeterGrpc.bindService(new GreeterImpl, system.dispatcher))

  "Stream client" - {
    "server->client stream" in {
      val call = new InboundStream(client, GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM, defaultOpts)
      val out = call(HelloRequestStream(10, "me"))
      val ex = out.toMat(Sink.seq)(Keep.right)

      implicit val mat = ActorMaterializer.create(system)

      val result = Await.result(ex.run(), 30.seconds)
      result should have length 10
    }
  }
}
