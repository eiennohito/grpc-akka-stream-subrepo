package org.eiennohito.grpc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.LoneElement

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/10/31
  */
abstract class GrpcAkkaSpec extends TestKit(ActorSystem()) with GrpcServerClientSpec with LoneElement {
  implicit def ec: ExecutionContext = system.dispatcher
  implicit lazy val mat = ActorMaterializer.create(system)

  override protected def afterAll() = {
    super.afterAll()
    system.terminate()
  }
}
