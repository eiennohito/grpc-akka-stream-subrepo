package org.eiennohito.grpc.stream.client

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.grpc._
import io.grpc.stub.ClientCalls
import org.eiennohito.grpc.stream.impl.{RequestOnceClientCall, SubscriberToStreamObserver}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.annotation.tailrec

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class InboundStream[Request, Reply](chan: Channel, md: MethodDescriptor[Request, Reply], opts: CallOptions) {
  def apply(req: Request): Source[Reply, NotUsed] = {
    val call = chan.newCall(md, opts)
    Source.fromPublisher(new Publisher[Reply] {
      override def subscribe(s: Subscriber[_ >: Reply]): Unit = {
        val subs = new ClientAdapterSubscription(call, 2)
        val wrapper = new RequestOnceClientCall(call, 2)
        val obs = SubscriberToStreamObserver[Reply](s)
        ClientCalls.asyncServerStreamingCall(wrapper, req, obs)
        s.onSubscribe(subs)
      }
    })
  }
}


class ClientAdapterSubscription(call: ClientCall[_, _], alreadyRequested: Int) extends Subscription {
  override def cancel() = call.cancel("cancelled by akka", null)
  private val requested = new AtomicInteger(alreadyRequested)

  @tailrec
  private def request0(n: Int): Unit = {
    val outstanding = requested.get()
    if (outstanding == 0) {
      call.request(n)
    } else {
      val update = outstanding min n

      if (!requested.compareAndSet(outstanding, outstanding - update)) {
        this.request0(n)
      } else {
        val canRequest = n - update
        if (canRequest > 0) {
          call.request(canRequest)
        }
      }
    }
  }

  override def request(n: Long): Unit = {
    if (n > 0) {
      request0(n.toInt)
    }
  }
}
