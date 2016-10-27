package org.eiennohito.grpc.stream.impl

import io.grpc.CallOptions
import org.eiennohito.grpc.stream.client.UnaryCall

import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/10/27
  */
class ClientUnaryCallImpl[Req, Resp] extends UnaryCall[Req, Resp] {
  override def withOpts(copts: CallOptions): UnaryCall[Req, Resp] = ???
  override def apply(v1: Req): Future[Resp] = ???
}
