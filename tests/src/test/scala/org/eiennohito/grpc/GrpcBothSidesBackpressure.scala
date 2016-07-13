package org.eiennohito.grpc

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.TestKit
import io.grpc.ServerServiceDefinition
import org.eiennohito.grpc.stream.client.InboundStream
import org.eiennohito.grpc.stream.server.ServiceBuilder

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * @author eiennohito
  * @since 2016/04/29
  */
class GrpcBothSidesBackpressure extends TestKit(ActorSystem()) with GrpcServerClientSpec {

  implicit lazy val amat = ActorMaterializer.create(system)
  implicit def ec: ExecutionContext = system.dispatcher

  override def init = { b =>
    val names = GreeterGrpc.METHOD_SAY_HELLO.getFullMethodName.split('/')
    val bldr = ServerServiceDefinition.builder(names(0))
    val bld2 = ServiceBuilder(bldr)

    bld2.method(GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM).handleWith(Service.svc)
    b.addService(bld2.result())
  }

  object Service {
    val counter = new AtomicLong(0L)

    def svc: Flow[HelloRequestStream, HelloReply, _] = {
      val message = (0 until 4096).map(_.toChar).toString()

      val src = Flow[HelloRequestStream]
      src.flatMapConcat(i => Source((0 until i.number).toStream.map(x => HelloReply(s"Hi, ${i.name}, #$x, $message"))))
          .map(o => {counter.incrementAndGet(); o })
    }
  }

  "BothSidesBackpressure" - {
    "do not eat all stream" in {
      val call = new InboundStream(client, GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM, defaultOpts)
      val stream = call(HelloRequestStream(500000, "me"))

      val data = stream.throttle(10, 30.milli, 2, ThrottleMode.Shaping).take(100).toMat(Sink.seq)(Keep.right)
      val results = Await.result(data.run(), 30.seconds)
      results.length shouldBe 100
      Service.counter.get should be <= 200L

      val call2 = call(HelloRequestStream(10, "me2"))
      val graph = call2.toMat(Sink.seq)(Keep.right)
      val results2 = Await.result(graph.run(), 30.seconds)
      results2 should have length 10
    }
  }
}


