package org.eiennohito.grpc.stream.impl

import io.grpc.ClientCall.Listener
import io.grpc.{ClientCall, Metadata}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class RequestOnceClientCall[R, T](wrapped: ClientCall[R, T], maxVal: Int = Int.MaxValue) extends ClientCall[R, T] {
  override def cancel(msg: String, t: Throwable) = wrapped.cancel(msg, t)
  override def halfClose() = wrapped.halfClose()

  @volatile private var firstTime = true
  override def request(numMessages: Int) = {
    if (firstTime) {
      firstTime = false
      wrapped.request(numMessages)
    }
  }

  override def sendMessage(message: R) = wrapped.sendMessage(message)

  override def start(responseListener: Listener[T], headers: Metadata) = wrapped.start(responseListener, headers)
}
