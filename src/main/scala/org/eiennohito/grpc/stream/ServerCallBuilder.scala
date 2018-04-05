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

package org.eiennohito.grpc.stream

import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import io.grpc._
import org.eiennohito.grpc.stream.impl.ServerAkkaStreamHandler
import org.eiennohito.grpc.stream.server.CallMetadata

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class ServerCallBuilder[Request: ClassTag, Reply](md: MethodDescriptor[Request, Reply])
    extends StrictLogging {
  def handleWith[T](flow: CallMetadata => Future[Flow[Request, Reply, T]])(
      implicit mat: Materializer,
      ec: ExecutionContext) = {
    val handler: ServerCallHandler[Request, Reply] =
      new ServerAkkaStreamHandler[Request, Reply](flow)
    handler
  }
}
