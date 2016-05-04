package org.eiennohito.grpc.stream

import io.grpc.MethodDescriptor


/**
  * @author eiennohito
  * @since 2016/05/04
  */
object GrpcNames {
  def svcName(md: MethodDescriptor[_, _]): String = {
    val name = md.getFullMethodName
    val parts = name.split('/')
    assert(parts.length == 2, s"length of $parts was not 2")
    parts(0)
  }
}
