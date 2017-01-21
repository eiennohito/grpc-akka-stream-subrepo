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

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import io.grpc.ServerServiceDefinition
import org.eiennohito.grpc.stream.impl.client.StreamInOneOutCallImpl
import org.eiennohito.grpc.stream.server.ServiceBuilder

class GrpcClientStreamingSpec extends TestKit(ActorSystem()) with GrpcServerClientSpec {

  implicit lazy val mat = ActorMaterializer.create(system)
  implicit def ec: ExecutionContext = system.dispatcher

  override def init = { b =>
    val names = GreeterGrpc.METHOD_SAY_HELLO.getFullMethodName.split('/')
    val bldr = ServerServiceDefinition.builder(names(0))
    val bld2 = ServiceBuilder(bldr)

    bld2.method(GreeterGrpc.METHOD_SAY_HELLO_CLIENT_STREAM).handleWith(Service.svc)
    b.addService(bld2.result())
  }

  object Service {
    def svc: Flow[HelloRequest, HelloStreamReply, _] = {
      Flow[HelloRequest]
        .fold(Seq.empty[String]) { case (acc, req) => acc :+ req.name}
        .map { seq => HelloStreamReply(seq.size, s"Hi, ${seq.mkString(", ")}") }
    }
  }

  "GrpcClientStreamingSpec" - {
    "client streaming" in {
      val call = new StreamInOneOutCallImpl(client, GreeterGrpc.METHOD_SAY_HELLO_CLIENT_STREAM, defaultOpts)
      val sink = call.apply()

      val source = Source(1 to 100).map(i => HelloRequest("a" + i))

      val (_, response) = source.toMat(sink)(Keep.right).run()

      val result = Await.result(response, 30.seconds)
      result.number shouldBe 100
    }
  }

  override protected def afterAll() = {
    system.terminate()
  }
}
