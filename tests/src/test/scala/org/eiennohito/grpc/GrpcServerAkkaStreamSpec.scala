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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.testkit.TestKit
import io.grpc.{MethodDescriptor, ServerServiceDefinition}
import org.eiennohito.grpc.stream.ServerCallBuilder
import org.eiennohito.grpc.stream.server.ServiceBuilder

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/04/29
  */
class GrpcServerAkkaStreamSpec extends GrpcAkkaSpec {

  override def init = { b => b.addService(AkkaServer.make(ActorMaterializer.create(system), system.dispatcher)) }

  "works" in {
    val syncStub = blocking(new GreeterGrpc.GreeterBlockingStub(_, _))
    val reply = syncStub.sayHello(HelloRequest("me"))
    reply.message shouldBe "Hi, me"
  }

  "server stream works" in {
    val syncStub = blocking(new GreeterGrpc.GreeterBlockingStub(_, _))
    val reply = syncStub.sayHelloSvrStream(HelloRequestStream(5, "me")).toList
    reply should have length 5
  }

  "server stream works with large number of msgs" in {
    val syncStub = blocking(new GreeterGrpc.GreeterBlockingStub(_, _))
    val reply = syncStub.sayHelloSvrStream(HelloRequestStream(5000, "me")).toList
    reply should have length 5000
  }
}

object AkkaServer {

  val simple: Flow[HelloRequest, HelloReply, NotUsed] = {
    Flow.fromFunction(x => HelloReply(s"Hi, ${x.name}"))
  }

  val serverStream: Flow[HelloRequestStream, HelloReply, NotUsed] = {
    Flow[HelloRequestStream].flatMapConcat(r => Source((0 until r.number).map { i => HelloReply(s"Hi, ${r.name} #$i")}))
  }

  def make(implicit mat: ActorMaterializer, ec: ExecutionContext): ServerServiceDefinition = {
    val names = GreeterGrpc.METHOD_SAY_HELLO.getFullMethodName.split('/')
    val bldr = ServerServiceDefinition.builder(names(0))
    val bld2 = ServiceBuilder(bldr)
    bld2.method(GreeterGrpc.METHOD_SAY_HELLO).handleWith(simple)
    bld2.method(GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM).handleWith(serverStream)
    bldr.build()
  }
}
