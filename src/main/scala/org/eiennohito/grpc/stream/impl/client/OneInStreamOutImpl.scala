package org.eiennohito.grpc.stream.impl.client

import akka.stream.scaladsl.{Flow, Keep, Source}
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import org.eiennohito.grpc.stream.client.{GrpcCallStatus, OneInStreamOutCall}

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class OneInStreamOutImpl[Req, Resp](chan: Channel, md: MethodDescriptor[Req, Resp], opts: CallOptions)
  extends OneInStreamOutCall[Req, Resp] {

  override def withOpts(cops: CallOptions): OneInStreamOutCall[Req, Resp] = {
    new OneInStreamOutImpl[Req, Resp](chan, md, cops)
  }

  override def apply(o: Req): Source[Resp, GrpcCallStatus] = {
    Source.single(o).viaMat(flow)(Keep.right)
  }

  override val flow = Flow.fromGraph(new GrpcClientHandler[Req, Resp](chan, md, opts))
}



