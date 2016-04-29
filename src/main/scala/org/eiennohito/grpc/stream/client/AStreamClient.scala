package org.eiennohito.grpc.stream.client

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import io.grpc.{CallOptions, Channel}


class AStreamChannel(val chan: Channel)

trait AStreamClient {

}

object AStreamClient extends AStreamClient

trait OneInStreamOutCall[T, R] {
  def apply(o: T): Source[R, NotUsed]
  def withOpts(o: T, cops: CallOptions): Source[R, NotUsed]
}


trait BidiStreamCall[T, R] {
  def apply(): Flow[T, R, NotUsed]
  def withOpts(copts: CallOptions): Flow[T, R, NotUsed]
}
