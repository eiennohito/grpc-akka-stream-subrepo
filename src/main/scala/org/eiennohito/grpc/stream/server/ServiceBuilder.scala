/*
 * Copyright 2016 eiennohito (Tolmachev Arseny)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eiennohito.grpc.stream.server

import akka.stream.Materializer
import akka.stream.scaladsl.Flow

import scalapb.grpc.ServiceCompanion
import io.grpc.ServerServiceDefinition.Builder
import io.grpc._
import org.eiennohito.grpc.stream.{GrpcNames, ServerCallBuilder}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/05/04
  */
class ServiceBuilder(private val bldr: ServerServiceDefinition.Builder) {
  
  def this(name: String) = this(ServerServiceDefinition.builder(name)) 
  
  def method[T: ClassTag,R](md: MethodDescriptor[T, R]) = {
    val scb = new ServerCallBuilder[T, R](md)
    new ServiceDefBuilder(bldr, md, scb)
  }

  def result(): ServerServiceDefinition = bldr.build()
}

object ServiceBuilder {
  def apply(service: ServiceDescriptor): ServiceBuilder = new ServiceBuilder(service.getName)

  @deprecated("use _Grpc.SERVICE instead", "20180308")
  def apply(grpc: ServiceCompanion[_]): ServiceBuilder = new ServiceBuilder(GrpcNames.svcName(grpc))
  def apply(name: String): ServiceBuilder = new ServiceBuilder(name)
  def apply(bldr: Builder): ServiceBuilder = new ServiceBuilder(bldr)
}


case class CallMetadata(ctx: Context, metadata: Metadata)

class ServiceDefBuilder[T, R](bldr: ServerServiceDefinition.Builder, mdesc: MethodDescriptor[T, R], scb: ServerCallBuilder[T, R]) {
  def handleSingle(fn: T => Future[R])(implicit mat: Materializer, ec: ExecutionContext): Unit = {
    val flow = Flow[T].mapAsync(1)(fn)
    handleWith(flow)
  }

  def handleWith[Mat](flow: Flow[T, R, Mat])(implicit mat: Materializer, ec: ExecutionContext): Unit = {
    handleWith(_ => Future.successful(flow))
  }

  def handleWith[Mat](flowFactory: CallMetadata => Future[Flow[T, R, Mat]])(implicit mat: Materializer, ec: ExecutionContext): Unit = {
    val sch = scb.handleWith(flowFactory)
    bldr.addMethod(mdesc, sch)
  }
}
