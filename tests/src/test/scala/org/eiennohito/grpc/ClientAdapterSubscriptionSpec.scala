package org.eiennohito.grpc

import io.grpc.ClientCall
import org.eiennohito.grpc.stream.client.ClientAdapterSubscription
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}

/**
  * @author eiennohito
  * @since 2016/04/29
  */
class ClientAdapterSubscriptionSpec extends FreeSpec with Matchers with MockFactory {
  "ClientAdapterSubscription" - {
    "works" in {
      val call = mock[ClientCall[AnyRef, AnyRef]]
      var cnt = 0
      (call.request _).expects(*).twice().onCall((x: Int) => cnt += x)
      val adapter = new ClientAdapterSubscription(call, 3)
      adapter.request(5)
      adapter.request(2)
      cnt shouldBe (5 + 2 - 3)
    }
  }

}
