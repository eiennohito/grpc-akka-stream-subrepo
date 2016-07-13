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
