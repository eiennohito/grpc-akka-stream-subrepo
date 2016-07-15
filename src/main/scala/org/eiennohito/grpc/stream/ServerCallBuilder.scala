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

import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.typesafe.scalalogging.StrictLogging
import io.grpc.MethodDescriptor.MethodType
import io.grpc._
import io.grpc.internal.GrpcUtil
import io.grpc.stub.ServerCalls.{ServerStreamingMethod, UnaryMethod}
import io.grpc.stub.{ServerCalls, StreamObserver}
import org.eiennohito.grpc.stream.adapters.{GrpcToSinkAdapter, ReadyHandler, ReadyInput, StreamObserverSinkOnce}
import org.eiennohito.grpc.stream.server.CallMetadata

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class ServerCallBuilder[Request, Reply](md: MethodDescriptor[Request, Reply]) extends StrictLogging {

  type FlowFactory[T] = CallMetadata => Future[Flow[Request, Reply, T]]

  private def asSinkOnce(obs: StreamObserver[Reply]) = {
    Sink.fromGraph(new StreamObserverSinkOnce(obs).named("GrpcSinkOnce"))
  }

  private def asSink(obs: StreamObserver[Reply], rinp: ReadyInput): Sink[Reply, Future[ReadyHandler]] = {
    Sink.fromGraph(new GrpcToSinkAdapter[Reply](obs, rinp).named("GrpcSink"))
  }

  private def wrapUnary[T](ffact: FlowFactory[T], mat: ActorMaterializer): UnaryMethod[Request, Reply] = {
    new UnaryMethod[Request, Reply] {
      override def invoke(request: Request, responseObserver: StreamObserver[Reply]) = {
        val context = Context.current()
        val meta = CallMetadata(context, null) //TODO: get normal metadata instead of this null

        val logic = ffact.apply(meta)

        logic.onComplete {
          case scala.util.Success(flow) =>
            val graph = GraphDSL.create(flow) { implicit b => f =>
              import GraphDSL.Implicits._
              val src = b.add(Source.single(request))
              val sink = b.add(asSinkOnce(responseObserver))
              src ~> f ~> sink
              ClosedShape
            }

            RunnableGraph.fromGraph(graph).run(mat)
          case scala.util.Failure(ex) =>
            logger.error("can't create flow", ex)
        }(mat.executionContext)
      }
    }
  }

  private def serverStreaming[T](ffact: FlowFactory[T])(implicit actorMaterializer: ActorMaterializer, ec: ExecutionContext): ServerCallHandler[Request, Reply] = {
    val handler = new ServerCallHandler[Request, Reply] {
      override def startCall(method: MethodDescriptor[Request, Reply], call: ServerCall[Reply], headers: Metadata) = {
        val compression = headers.get(GrpcUtil.MESSAGE_ENCODING_KEY)
        if (compression != null && compression != "") {
          call.setMessageCompression(true)
          call.setCompression(compression)
        }

        new ServerCall.Listener[Request] {
          private val readyHandler = Promise[ReadyHandler]

          private val ssm = new ServerStreamingMethod[Request, Reply] with ReadyInput {
            override def invoke(request: Request, responseObserver: StreamObserver[Reply]) = {
              val ctx = Context.current()
              val logicF = ffact.apply(CallMetadata(ctx, headers))
              logicF.onComplete {
                case scala.util.Success(flow) =>
                  try {
                    val source = Source.single(request)
                    val sink: Sink[Reply, Future[ReadyHandler]] = asSink(responseObserver, this).async
                    val (_, res) = flow.runWith(source, sink)
                    readyHandler.completeWith(res)
                  } catch {
                    case e: Exception =>
                      responseObserver.onError(Status.INTERNAL.withCause(e).asException())
                      readyHandler.failure(e)
                  }
                case scala.util.Failure(e) =>
                  responseObserver.onError(Status.INTERNAL.withCause(e).asException())
                  readyHandler.failure(e)
              }
            }

            override def isReady = call.isReady
          }

          private val handler = ServerCalls.asyncServerStreamingCall(ssm)
          private val listener = handler.startCall(method, call, headers)

          private val readyFuture = readyHandler.future

          override def onMessage(message: Request) = {
            listener.onMessage(message)
          }
          override def onComplete() = listener.onComplete()
          override def onHalfClose() = listener.onHalfClose()

          override def onCancel() = {
            if (readyFuture.isCompleted) {
              readyFuture.value.foreach(_.get.onCancel())
            } else {
              readyFuture.foreach(_.onCancel())
            }
            listener.onCancel()
          }

          override def onReady() = {
            readyFuture.foreach(_.onReady())
            listener.onReady()
          }
        }
      }
    }
    handler
  }

  def handleWith[T](flow: CallMetadata => Future[Flow[Request, Reply, T]])(implicit mat: ActorMaterializer, ec: ExecutionContext) = {
    val handler: ServerCallHandler[Request, Reply] = md.getType match {
      case MethodType.UNARY => ServerCalls.asyncUnaryCall(wrapUnary(flow, mat))
      case MethodType.CLIENT_STREAMING => ???
      case MethodType.SERVER_STREAMING => serverStreaming(flow)
      case MethodType.BIDI_STREAMING => ???
      case MethodType.UNKNOWN => ???
      case _ => throw new Exception()
    }

    handler
  }
}
