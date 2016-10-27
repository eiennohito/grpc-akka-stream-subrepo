package org.eiennohito.grpc.stream

import akka.stream.scaladsl.Flow
import org.eiennohito.grpc.stream.server.CallMetadata

import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/10/27
  */
object GrpcStreaming {
  type ServerHandler[Req, Resp, Mat] = CallMetadata => Future[Flow[Req, Resp, Mat]]
}
