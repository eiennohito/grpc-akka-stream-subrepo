package org.eiennohito.grpc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import org.eiennohito.grpc.stream.impl.client.UnaryCallImpl
import org.eiennohito.grpc.stream.server.ServiceBuilder

import scala.concurrent.{Await, ExecutionContext}

/**
  * @author eiennohito
  * @since 2016/10/28
  */
class ExceptionsArePropagatedToClient extends TestKit(ActorSystem()) with GrpcServerClientSpec {
  import scala.concurrent.duration._

  implicit lazy val amat = ActorMaterializer.create(system)
  implicit def ec: ExecutionContext = system.dispatcher
  override def init = { _.addService(failSvc) }

  def failSvc = {
    val bldr = ServiceBuilder(GreeterGrpc.Greeter)
    bldr.method(GreeterGrpc.METHOD_SAY_HELLO).handleWith(Flow[HelloRequest].map(_ => throw new Exception("fail!")))
    bldr.result()
  }

  "ExceptionsArePropagated" - {
    "works" in {
      val call = new UnaryCallImpl(client, GreeterGrpc.METHOD_SAY_HELLO, defaultOpts)
      val f = call(HelloRequest("hello!"))
      Await.ready(f, 10.seconds)
      val thr = f.value.get.failed.get
      val ex = thr.getSuppressed()(0)
      ex.getMessage shouldBe "fail!"
    }
  }
}
