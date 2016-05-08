package org.eiennohito.grpc.stream.client

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import io.grpc.{CallOptions, Channel, MethodDescriptor}


class AStreamChannel(val chan: Channel)

trait AStreamClient {}
trait AClientCompanion[T <: AStreamClient] {
  def name: String
  def build(channel: Channel, callOptions: CallOptions): T
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
  def serverStream[T, R](md: MethodDescriptor[T, R]): OneInStreamOutCall[T, R] = new InboundStream[T, R](chan, md, callOptions)
}

object ClientBuilder {
  def apply(chan: Channel, callOptions: CallOptions): ClientBuilder = new ClientBuilder(chan, callOptions)
}
