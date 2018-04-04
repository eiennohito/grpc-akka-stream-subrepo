package org.eiennohito.grpc

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import org.eiennohito.grpc.stream.client.ClientBuilder
import org.eiennohito.grpc.stream.impl.ScalaMetadata
import org.eiennohito.grpc.stream.server.ServiceBuilder

import scala.concurrent.Await

/**
  * @author eiennohito
  * @since 2016/10/28
  */
class IdPropagationSpec extends GrpcAkkaSpec {
  import scala.concurrent.duration._

  override def init = { _.addService(serverSvc) }

  lazy val clientImpl = {
    val bldr = ClientBuilder(client, defaultOpts)
    bldr.unary(GreeterGrpc.METHOD_SAY_HELLO)
  }

  def serverSvc = {
    val bldr = ServiceBuilder(GreeterGrpc.SERVICE)

    val stage = Source.fromGraph(new IdGraphStage)
    bldr.method(GreeterGrpc.METHOD_SAY_HELLO)
      .handleWith(
        Flow[HelloRequest]
          .flatMapMerge(1, _ => stage)
          .map(s => HelloReply(s))
      )

    bldr.method(GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM).handleWith(
      Flow[HelloRequestStream]
        .mapConcat(rs => List.fill(rs.number)(HelloRequest(rs.name)))
        .flatMapMerge(4, r => Source.single(r).via(clientImpl.flow)))
    bldr.result()
  }

  "IdPropagation" - {
    "works" ignore { //TODO: akka do not propagate attributes to dynamically created streams https://github.com/akka/akka/issues/21743
      val call = ClientBuilder(client, defaultOpts).serverStream(GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM)
      val (status, objsF) = call(HelloRequestStream(100, "me")).toMat(Sink.seq)(Keep.both).run()
      val objs = Await.result(objsF, 100.seconds)
      objs should have length (100)
      objs.distinct.loneElement shouldBe status.id
    }
  }

  override protected def afterAll() = {
    super.afterAll()
    system.terminate()
  }
}

class IdGraphStage extends GraphStage[SourceShape[String]] {
  val out = Outlet[String]("out")
  val shape = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    val attr = inheritedAttributes.get[ScalaMetadata.InitialRequestId]
    new GraphStageLogic(shape) with OutHandler {
      setHandler(out, this)

      private val res = attr.get.id.toString

      override def onPull(): Unit = {
        push(out, res)
      }
    }
  }
}
