package org.eiennohito.grpc

import java.util.UUID

import org.eiennohito.grpc.stream.impl.ScalaMetadata.{ThrowableMarshaller, UUIDMarshaller}
import org.scalatest.{FreeSpec, Matchers}

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class MarshallerSpec extends FreeSpec with Matchers {
  "UUID marshaller" - {
    "works" in {
      for (_ <- 0 until 1000) {
        val u = UUID.randomUUID()
        val toBytes = UUIDMarshaller.toBytes(u)
        val ru = UUIDMarshaller.parseBytes(toBytes)
        ru shouldBe u
      }
    }
  }

  "Throwable marshaller" - {
    "works" in {
      val ex = new Exception("the message")
      val bytes = ThrowableMarshaller.toBytes(ex)
      val obj = ThrowableMarshaller.parseBytes(bytes)
      ex.getMessage shouldBe obj.getMessage
    }
  }
}
