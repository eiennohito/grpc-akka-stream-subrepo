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

import java.util.UUID

import akka.stream.Materializer
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.grpc.MethodDescriptor.MethodType
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import org.eiennohito.grpc.stream.impl.client.{OneInStreamOutImpl, StreamInOneOutCallImpl, UnaryCallImpl}
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

trait AStreamCall[T, R] {
  def flow: Flow[T, R, GrpcCallStatus]
}

trait UnaryCall[T, R] extends AStreamCall[T, R] {
  def withOpts(copts: CallOptions): UnaryCall[T, R]
  def apply(v: T)(implicit mat: Materializer): Future[R]
  def func(implicit mat: Materializer): T => Future[R] = apply
}

trait OneInStreamOutCall[T, R] extends (T => Source[R, GrpcCallStatus]) with AStreamCall[T, R] {
  def apply(o: T): Source[R, GrpcCallStatus]
  def withOpts(cops: CallOptions): OneInStreamOutCall[T, R]
}

trait StreamInOneOutCall[T, R] extends (() => Sink[T, (GrpcCallStatus, Future[R])]) with AStreamCall[T, R] {
  def apply(): Sink[T, (GrpcCallStatus, Future[R])]
  def withOpts(cops: CallOptions): StreamInOneOutCall[T, R]
}

trait BidiStreamCall[T, R] {
  def apply(): Flow[T, R, NotUsed]
  def withOpts(copts: CallOptions): BidiStreamCall[T, R]
}

case class GrpcCallStatus(id: UUID, headers: Future[Metadata], trailers: Future[Metadata], completion: Future[Done])

class ClientBuilder(chan: Channel, callOptions: CallOptions) {
  def serverStream[T, R](md: MethodDescriptor[T, R]): OneInStreamOutCall[T, R] = {
    assert(md.getType == MethodType.SERVER_STREAMING)
    new OneInStreamOutImpl[T, R](chan, md, callOptions)
  }

  def clientStream[T, R](md: MethodDescriptor[T, R]): StreamInOneOutCall[T, R] = {
    assert(md.getType == MethodType.CLIENT_STREAMING)
    new StreamInOneOutCallImpl[T, R](chan, md, callOptions)
  }

  def unary[T, R](md: MethodDescriptor[T, R]): UnaryCall[T, R] = {
    assert(md.getType == MethodType.UNARY)
    new UnaryCallImpl[T, R](chan, md, callOptions)
  }
}

object ClientBuilder {
  def apply(chan: Channel, callOptions: CallOptions): ClientBuilder = new ClientBuilder(chan, callOptions)
}
