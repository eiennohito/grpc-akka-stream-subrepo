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

package org.eiennohito.grpc.stream.client

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import io.grpc._
import io.grpc.stub.{ClientCalls, StreamObserver}
import org.eiennohito.grpc.stream.impl.{RequestOnceClientCall, SubscriberToStreamObserver}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.annotation.tailrec
import scala.concurrent.Promise

/**
  * @author eiennohito
  * @since 2016/04/28
  */
class InboundStream[Request, Reply](chan: Channel, md: MethodDescriptor[Request, Reply], opts: CallOptions) extends OneInStreamOutCall[Request, Reply] {
  def apply(req: Request): Source[Reply, NotUsed] = {
    withOpts(req, opts)
  }

  override def withOpts(o: Request, cops: CallOptions) = {
    val call = chan.newCall(md, cops)
    Source.fromPublisher(new Publisher[Reply] {
      override def subscribe(s: Subscriber[_ >: Reply]): Unit = {
        val subs = new ClientAdapterSubscription(call, 2)
        val wrapper = new RequestOnceClientCall(call, 2)
        val obs = SubscriberToStreamObserver[Reply](s)
        ClientCalls.asyncServerStreamingCall(wrapper, o, obs)
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

class SingularCallImpl[Request, Reply](chan: Channel, md: MethodDescriptor[Request, Reply], opts: CallOptions)
  extends UnaryCall[Request, Reply] with StrictLogging {
  override def withOpts(copts: CallOptions) = {
    val wrapped = new SingularCallImpl(chan, md, copts)
    wrapped
  }

  override def apply(v1: Request) = {
    val call = chan.newCall(md, opts)
    val promise = Promise[Reply]
    ClientCalls.asyncUnaryCall(call, v1, new StreamObserver[Reply] {
      override def onError(t: Throwable) = {
        if (!promise.tryFailure(t)) {
          logger.warn(s"could not finish call $md, was already completed", t)
        }
      }
      override def onCompleted() = {
        if (!promise.isCompleted) {
          promise.failure(new Exception(s"no value in call $md"))
        }
      }
      override def onNext(value: Reply) = {
        if (!promise.trySuccess(value)) {
          logger.warn(s"could not complete unary call $md")
        }
      }
    })

    promise.future
  }
}
