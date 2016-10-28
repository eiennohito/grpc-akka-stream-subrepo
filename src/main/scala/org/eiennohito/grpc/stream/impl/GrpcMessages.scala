package org.eiennohito.grpc.stream.impl

import io.grpc.{Metadata, Status}

/**
  * @author eiennohito
  * @since 2016/10/27
  */
object GrpcMessages {
  case object StopRequests
  case object Complete
  case object Ready
  case class Close(status: Status, trailers: Metadata)
  case class Headers(headers: Metadata)
}
