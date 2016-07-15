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

package org.eiennohito.grpc.stream.client

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import io.grpc.MethodDescriptor.MethodType
import io.grpc.{CallOptions, Channel, MethodDescriptor}

import scala.concurrent.Future


class AStreamChannel(val chan: Channel)

trait AStreamClient {}

trait AClientFactory {
  type Service <: AStreamClient
  def name: String
  def build(channel: Channel, callOptions: CallOptions): Service
}

trait AClientCompanion[T <: AStreamClient] extends AClientFactory {
  type Service = T
}

trait UnaryCall[T, R] extends (T => Future[R]) {
  def withOpts(copts: CallOptions): (T => Future[R])
}

trait OneInStreamOutCall[T, R] extends (T => Source[R, NotUsed]) {
  def apply(o: T): Source[R, NotUsed]
  def withOpts(o: T, cops: CallOptions): Source[R, NotUsed]
}


trait BidiStreamCall[T, R] {
  def apply(): Flow[T, R, NotUsed]
  def withOpts(copts: CallOptions): Flow[T, R, NotUsed]
}

class ClientBuilder(chan: Channel, callOptions: CallOptions) {
  def serverStream[T, R](md: MethodDescriptor[T, R]): OneInStreamOutCall[T, R] = {
    assert(md.getType == MethodType.SERVER_STREAMING)
    new InboundStream[T, R](chan, md, callOptions)
  }

  def unary[T, R](md: MethodDescriptor[T, R]): UnaryCall[T, R] = {
    assert(md.getType == MethodType.UNARY)
    new SingularCallImpl[T, R](chan, md, callOptions)
  }
}

object ClientBuilder {
  def apply(chan: Channel, callOptions: CallOptions): ClientBuilder = new ClientBuilder(chan, callOptions)
}
