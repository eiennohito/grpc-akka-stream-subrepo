package org.eiennohito.grpc.stream.server

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.trueaccord.scalapb.grpc.ServiceCompanion
import io.grpc.ServerServiceDefinition.Builder
import io.grpc.{Context, Metadata, MethodDescriptor, ServerServiceDefinition}
import org.eiennohito.grpc.stream.{GrpcNames, ServerCallBuilder}

import scala.concurrent.{ExecutionContext, Future}

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
  def apply(grpc: ServiceCompanion): ServiceBuilder = new ServiceBuilder(GrpcNames.svcName(grpc))
  def apply(name: String): ServiceBuilder = new ServiceBuilder(name)
  def apply(bldr: Builder): ServiceBuilder = new ServiceBuilder(bldr)
}


case class CallMetadata(ctx: Context, metadata: Metadata)

class ServiceDefBuilder[T, R](bldr: ServerServiceDefinition.Builder, mdesc: MethodDescriptor[T, R], scb: ServerCallBuilder[T, R]) {
  def handleWith[Mat](flow: Flow[T, R, Mat])(implicit mat: ActorMaterializer, ec: ExecutionContext): Unit = {
    handleWith(_ => Future.successful(flow))
  }

  def handleWith[Mat](flowFactory: CallMetadata => Future[Flow[T, R, Mat]])(implicit mat: ActorMaterializer, ec: ExecutionContext): Unit = {
    val sch = scb.handleWith(flowFactory)
    bldr.addMethod(mdesc, sch)
  }
}
