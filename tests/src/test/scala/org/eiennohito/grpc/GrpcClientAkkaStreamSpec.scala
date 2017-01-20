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

package org.eiennohito.grpc

import akka.stream.scaladsl.{Keep, Sink, Source}
import org.eiennohito.grpc.stream.impl.client.{OneInStreamOutImpl, StreamInOneOutCallImpl}
import scala.concurrent.Await
import scala.concurrent.duration._
/**
  * @author eiennohito
  * @since 2016/04/29
  */
class GrpcClientAkkaStreamSpec extends GrpcAkkaSpec {
  override def init = _.addService(GreeterGrpc.bindService(new GreeterImpl, system.dispatcher))

  "Stream client" - {
    "server->client stream" in {
      val call = new OneInStreamOutImpl(client, GreeterGrpc.METHOD_SAY_HELLO_SVR_STREAM, defaultOpts)
      val source = call(HelloRequestStream(10, "me"))
      val ex = source.toMat(Sink.seq)(Keep.right)
      val result = Await.result(ex.run(), 30.seconds)
      result should have length 10
    }

    "client->server stream" in {
      val call = new StreamInOneOutCallImpl(client, GreeterGrpc.METHOD_SAY_HELLO_CLIENT_STREAM, defaultOpts)
      val sink = call.apply()
      val source = Source(1 to 10).map(i => HelloRequest("a" + i))
      val ex = source.toMat(sink)(Keep.right)
      val result = Await.result(ex.run()._2, 30.seconds)
      result.number shouldBe 10
    }
  }
}


