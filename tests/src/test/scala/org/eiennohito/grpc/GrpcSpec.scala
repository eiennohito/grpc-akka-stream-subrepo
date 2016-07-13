package org.eiennohito.grpc

import java.util.concurrent.TimeUnit

import io.grpc.stub.AbstractStub
import io.grpc._
import org.eiennohito.util.Jul2Logback
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

import scala.concurrent.ExecutionContext
import scala.util.Random

trait GrpcServerClientSpec extends FreeSpecLike with Matchers with BeforeAndAfterAll {

  def init: ServerBuilder[_] => Unit

  Jul2Logback.initialized

  val (server, port) = {
    GrpcServer.makeServer(init)
  }

  val client = {
    val bldr = ManagedChannelBuilder.forAddress("localhost", port)
    bldr.usePlaintext(true)
    bldr.executor(ExecutionContext.global)
    bldr.build()
  }

  def blocking[T <: AbstractStub[T]](f: (Channel, CallOptions) => T): T = {
    f(client, defaultOpts)
  }

  def defaultOpts: CallOptions = {
    CallOptions.DEFAULT.withDeadlineAfter(30, TimeUnit.SECONDS)
  }

  override protected def afterAll() = {
    super.afterAll()
    client.shutdown()
    client.awaitTermination(1, TimeUnit.MINUTES)
    server.shutdown()
  }
}

object GrpcServer {
  private def makeServer0(f: ServerBuilder[_] => Unit, trial: Int, maxTrial: Int): (Server, Int) = {
    try {
      val port = Random.nextInt(10000) + 50000
      val sb = ServerBuilder.forPort(port)
      f(sb)
      val srv = sb.build()
      srv.start()
      (srv, port)
    } catch {
      case e: Exception =>
        if (trial >= maxTrial) {
          throw e
        } else {
          makeServer0(f, trial + 1, maxTrial)
        }
    }
  }

  def makeServer(f: ServerBuilder[_] => Unit): (Server, Int) = {
    makeServer0(f, 0, 5)
  }
}


