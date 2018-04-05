package org.eiennohito.grpc.stream.impl.client

import akka.stream.scaladsl.Flow
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import org.eiennohito.grpc.stream.client.{BidiStreamCall, GrpcCallStatus}

class BidiCallImpl[Request, Reply](
    chan: Channel,
    md: MethodDescriptor[Request, Reply],
    callOpts: CallOptions)
    extends BidiStreamCall[Request, Reply] {
  override val flow: Flow[Request, Reply, GrpcCallStatus] = {
    Flow.fromGraph(new GrpcClientHandler[Request, Reply](chan, md, callOpts))
  }

  override def withOpts(copts: CallOptions): BidiStreamCall[Request, Reply] =
    new BidiCallImpl(chan, md, copts)
}
