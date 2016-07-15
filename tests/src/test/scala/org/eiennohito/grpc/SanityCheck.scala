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

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class SanityCheck extends GrpcServerClientSpec {
  "GrpcSimple" - {
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
  }

  override def init = _.addService(GreeterGrpc.bindService(new GreeterImpl, ExecutionContext.global))
}
