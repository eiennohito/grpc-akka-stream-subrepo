package org.eiennohito.grpc

import akka.stream.scaladsl.Flow
import org.eiennohito.grpc.stream.impl.client.UnaryCallImpl
import org.eiennohito.grpc.stream.server.ServiceBuilder

import scala.concurrent.Await

/**
  * @author eiennohito
  * @since 2016/10/28
  */
class ExceptionsArePropagatedToClient extends GrpcAkkaSpec {
  import scala.concurrent.duration._

  override def init = { _.addService(failSvc) }

  def failSvc = {
    val bldr = ServiceBuilder(GreeterGrpc.SERVICE)
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
