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
