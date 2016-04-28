package org.eiennohito.grpc.stream

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.grpc.ClientCall.Listener
import io.grpc._
import io.grpc.MethodDescriptor.MethodType
import io.grpc.stub.ServerCalls.{ServerStreamingMethod, UnaryMethod}
import io.grpc.stub.{ClientCalls, ServerCalls, StreamObserver}
import org.eiennohito.grpc.stream.adapters.{GrpcToSinkAdapter, ReadyHandler, ReadyInput, StreamObserverSinkOnce}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class ServerCallBuilder[Request, Reply](md: MethodDescriptor[Request, Reply]) {
  def asSinkOnce(obs: StreamObserver[Reply]) = {
    Sink.fromGraph(new StreamObserverSinkOnce(obs).named("GrpcSinkOnce"))
  }

  def asSink(obs: StreamObserver[Reply], rinp: ReadyInput): Sink[Reply, ReadyHandler] = {
    Sink.fromGraph(new GrpcToSinkAdapter[Reply](obs, rinp).named("GrpcSink"))
  }

  def wrapUnary[T](flow: Flow[Request, Reply, T], mat: ActorMaterializer): UnaryMethod[Request, Reply] = {
    new UnaryMethod[Request, Reply] {
      override def invoke(request: Request, responseObserver: StreamObserver[Reply]) = {
        val source = Source.single(request)
        val sink = asSinkOnce(responseObserver)
        flow.runWith(source, sink)(mat)
      }
    }
  }

  private def serverStreaming[T](flow: Flow[Request, Reply, T])(implicit actorMaterializer: ActorMaterializer): ServerCallHandler[Request, Reply] = {
    val handler = new ServerCallHandler[Request, Reply] {
      override def startCall(method: MethodDescriptor[Request, Reply], call: ServerCall[Reply], headers: Metadata) = {
        new ServerCall.Listener[Request] {
          private var readyHandler: ReadyHandler = null

          private val ssm = new ServerStreamingMethod[Request, Reply] with ReadyInput {
            override def invoke(request: Request, responseObserver: StreamObserver[Reply]) = {
              val source = Source.single(request)
              val sink: Sink[Reply, ReadyHandler] = asSink(responseObserver, this)
              val (_, res) = flow.runWith(source, sink)
              readyHandler = res
            }

            override def isReady = call.isReady
          }

          private val handler = ServerCalls.asyncServerStreamingCall(ssm)
          private val listener = handler.startCall(method, call, headers)


          override def onMessage(message: Request) = listener.onMessage(message)
          override def onCancel() = {
            readyHandler.onCancel()
            listener.onCancel()
          }
          override def onComplete() = listener.onComplete()
          override def onReady() = {
            listener.onReady()
            readyHandler.onReady()
          }
          override def onHalfClose() = listener.onHalfClose()
        }
      }
    }
    handler
  }

  def handleWith[T](flow: Flow[Request, Reply, T])(implicit mat: ActorMaterializer) = {
    val handler: ServerCallHandler[Request, Reply] = md.getType match {
      case MethodType.UNARY => ServerCalls.asyncUnaryCall(wrapUnary(flow, mat))
      case MethodType.CLIENT_STREAMING => ???
      case MethodType.SERVER_STREAMING => serverStreaming(flow)
      case MethodType.BIDI_STREAMING => ???
      case MethodType.UNKNOWN => throw new Exception()
      case _ => throw new Exception()
    }

    handler
  }
}
