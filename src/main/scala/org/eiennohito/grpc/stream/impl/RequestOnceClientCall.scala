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

package org.eiennohito.grpc.stream.impl

import io.grpc.ClientCall.Listener
import io.grpc.{ClientCall, Metadata}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class RequestOnceClientCall[R, T](wrapped: ClientCall[R, T], maxVal: Int = Int.MaxValue) extends ClientCall[R, T] {
  override def cancel(msg: String, t: Throwable) = wrapped.cancel(msg, t)
  override def halfClose() = wrapped.halfClose()

  @volatile private var firstTime = true
  override def request(numMessages: Int) = {
    if (firstTime) {
      firstTime = false
      wrapped.request(numMessages)
    }
  }

  override def sendMessage(message: R) = wrapped.sendMessage(message)

  override def start(responseListener: Listener[T], headers: Metadata) = wrapped.start(responseListener, headers)
}
