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


