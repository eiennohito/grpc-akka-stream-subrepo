package org.eiennohito.grpc.stream.server

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import io.grpc.ServerServiceDefinition.Builder
import io.grpc.{MethodDescriptor, ServerServiceDefinition}
import org.eiennohito.grpc.stream.ServerCallBuilder

import scala.concurrent.ExecutionContext

/**
  * @author eiennohito
  * @since 2016/05/04
  */
class ServiceBuilder(private val bldr: ServerServiceDefinition.Builder) {
  
  def this(name: String) = this(ServerServiceDefinition.builder(name)) 
  
  def method[T,R](md: MethodDescriptor[T, R]) = {
    val scb = new ServerCallBuilder[T, R](md)
    new ServiceDefBuilder(bldr, md, scb)
  }

  def result(): ServerServiceDefinition = bldr.build()
}

object ServiceBuilder {
  def apply(name: String): ServiceBuilder = new ServiceBuilder(name)
  def apply(bldr: Builder): ServiceBuilder = new ServiceBuilder(bldr)
}


class ServiceDefBuilder[T, R](bldr: ServerServiceDefinition.Builder, mdesc: MethodDescriptor[T, R], scb: ServerCallBuilder[T, R]) {
  def handleWith[Mat](flow: Flow[T, R, Mat])(implicit mat: ActorMaterializer, ec: ExecutionContext): Unit = {
    val sch = scb.handleWith(flow)
    bldr.addMethod(mdesc, sch)
  }
}
