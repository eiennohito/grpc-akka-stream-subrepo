package org.eiennohito.grpc.stream.impl

import io.grpc.stub.StreamObserver
import org.reactivestreams.Subscriber

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class SubscriberToStreamObserver[T](s: Subscriber[T]) extends StreamObserver[T] {
  override def onCompleted() = s.onComplete()
  override def onError(t: Throwable) = s.onError(t)
  override def onNext(value: T) = s.onNext(value)
}
