package org.eiennohito.grpc.stream.client

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import io.grpc.MethodDescriptor.MethodType
import io.grpc.{CallOptions, Channel, MethodDescriptor}

import scala.reflect.internal.Types.MethodType


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

trait OneInStreamOutCall[T, R] {
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
}

object ClientBuilder {
  def apply(chan: Channel, callOptions: CallOptions): ClientBuilder = new ClientBuilder(chan, callOptions)
}
