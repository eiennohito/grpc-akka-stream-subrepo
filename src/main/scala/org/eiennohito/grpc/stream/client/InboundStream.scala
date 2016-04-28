package org.eiennohito.grpc.stream.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.grpc._
import io.grpc.stub.ClientCalls
import org.eiennohito.grpc.stream.impl.{RequestOnceClientCall, SubscriberToStreamObserver}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class InboundStream[Request, Reply](chan: Channel, md: MethodDescriptor[Request, Reply], opts: CallOptions) {
  def apply(req: Request): Source[Reply, NotUsed] = {
    val call = chan.newCall(md, opts)
    Source.fromPublisher(new Publisher[Reply] {
      override def subscribe(s: Subscriber[_ >: Reply]): Unit = {
        val subs = new Subscription {
          override def cancel() = call.cancel()
          override def request(n: Long) = call.request(n.toInt)
        }

        val wrapper = new RequestOnceClientCall(call, 2)
        val obs = SubscriberToStreamObserver[Reply](s)
        s.onSubscribe(subs)

        ClientCalls.asyncServerStreamingCall(wrapper, req, obs)
      }
    })
  }
}


