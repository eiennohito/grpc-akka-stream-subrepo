package org.eiennohito.grpc.stream

import com.trueaccord.scalapb.grpc.ServiceCompanion
import io.grpc.MethodDescriptor


/**
  * @author eiennohito
  * @since 2016/05/04
  */
object GrpcNames {
  def svcName(md: MethodDescriptor[_, _]): String = {
    MethodDescriptor.extractFullServiceName(md.getFullMethodName)
  }

  def svcName(svc: ServiceCompanion): String = {
    svc.descriptor.getName
  }
}
