package org.eiennohito.grpc

import io.grpc.stub.StreamObserver
import org.eiennohito.grpc.GreeterGrpc.Greeter

import scala.concurrent.Future

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class GreeterImpl extends Greeter {
  override def sayHello(request: HelloRequest) = {
    Future.successful(HelloReply("Hi, " + request.name))
  }

  override def sayHelloSvrStream(request: HelloRequestStream, responseObserver: StreamObserver[HelloReply]) = {
    for (i <- 0 until request.number) {
      responseObserver.onNext(HelloReply(s"Hi, ${request.name}, #$i"))
    }
    responseObserver.onCompleted()
  }
}
