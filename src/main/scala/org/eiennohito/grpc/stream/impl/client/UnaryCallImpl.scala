package org.eiennohito.grpc.stream.impl.client

import akka.stream.{Materializer, Fusing}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import org.eiennohito.grpc.stream.client.{GrpcCallStatus, UnaryCall}

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class UnaryCallImpl[Request, Reply](
  chan: Channel,
  md: MethodDescriptor[Request, Reply],
  opts: CallOptions)
  extends UnaryCall[Request, Reply] with StrictLogging {
  override def withOpts(copts: CallOptions) = {
    val wrapped = new UnaryCallImpl(chan, md, copts)
    wrapped
  }

  override val flow: Flow[Request, Reply, GrpcCallStatus] = {
    val flow = Flow[Request]
      .take(1)
      .viaMat(Flow.fromGraph(new GrpcClientHandler[Request, Reply](chan, md, opts)))(Keep.right)
      .take(1)
    Flow.fromGraph(Fusing.aggressive(flow))
  }

  override def apply(v1: Request)(implicit mat: Materializer) = {
    Source.single(v1).via(flow).runWith(Sink.head)
  }
}
