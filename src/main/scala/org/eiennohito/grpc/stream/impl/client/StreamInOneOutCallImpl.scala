package org.eiennohito.grpc.stream.impl.client

import scala.concurrent.Future

import akka.stream.scaladsl.{Flow, Keep, Sink}
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import org.eiennohito.grpc.stream.client.{GrpcCallStatus, StreamInOneOutCall}

class StreamInOneOutCallImpl[Req, Resp](chan: Channel, md: MethodDescriptor[Req, Resp], opts: CallOptions)
  extends StreamInOneOutCall[Req, Resp] {

  override def withOpts(cops: CallOptions): StreamInOneOutCall[Req, Resp] = {
    new StreamInOneOutCallImpl[Req, Resp](chan, md, cops)
  }

  override def apply(): Sink[Req, (GrpcCallStatus, Future[Resp])] =
    flow.toMat(Sink.head)(Keep.both)

  override def flow: Flow[Req, Resp, GrpcCallStatus] =
    Flow.fromGraph(new GrpcClientHandler[Req, Resp](chan, md, opts))
}
