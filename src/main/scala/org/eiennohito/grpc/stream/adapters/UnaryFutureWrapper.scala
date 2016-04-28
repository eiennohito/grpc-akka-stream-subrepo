package org.eiennohito.grpc.stream.adapters

import io.grpc.stub.ServerCalls.UnaryMethod
import io.grpc.stub.StreamObserver

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class UnaryFutureWrapper[R, T](fn: R => Future[T])(implicit ec: ExecutionContext) extends UnaryMethod[R, T] {
  override def invoke(request: R, obs: StreamObserver[T]): Unit = {
    fn(request).onComplete {
      case scala.util.Success(s) =>
        obs.onNext(s)
        obs.onCompleted()
      case scala.util.Failure(f) =>
        obs.onError(f)
    }
  }
}
