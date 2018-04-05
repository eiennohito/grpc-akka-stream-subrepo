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

package org.eiennohito.grpc.stream.adapters

import io.grpc.stub.ServerCalls.UnaryMethod
import io.grpc.stub.StreamObserver

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class UnaryFutureWrapper[R, T](fn: R => Future[T])(implicit ec: ExecutionContext)
    extends UnaryMethod[R, T] {
  override def invoke(request: R, obs: StreamObserver[T]): Unit = {
    fn(request).onComplete {
      case scala.util.Success(s) =>
        obs.onNext(s)
        obs.onCompleted()
      case scala.util.Failure(f) =>
        obs.onError(f)
    }
  }
}
