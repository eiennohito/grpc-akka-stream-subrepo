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
