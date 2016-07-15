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

import io.grpc.stub.StreamObserver
import org.reactivestreams.Subscriber

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class SubscriberToStreamObserver[T](s: Subscriber[_ >: T]) extends StreamObserver[T] {
  override def onCompleted() = s.onComplete()
  override def onError(t: Throwable) = s.onError(t)
  override def onNext(value: T) = s.onNext(value)
}

object SubscriberToStreamObserver {
  def apply[T](s: Subscriber[_ >: T]) = new SubscriberToStreamObserver[T](s)
}
