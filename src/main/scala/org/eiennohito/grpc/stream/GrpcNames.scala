package org.eiennohito.grpc.stream

import io.grpc.MethodDescriptor


/**
  * @author eiennohito
  * @since 2016/05/04
  */
object GrpcNames {
  def svcName(md: MethodDescriptor[_, _]): String = {
    MethodDescriptor.extractFullServiceName(md.getFullMethodName)
  }
}
