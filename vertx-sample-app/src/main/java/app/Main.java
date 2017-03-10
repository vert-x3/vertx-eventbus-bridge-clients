/*
 * Copyright 2016 Julien Ponge
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

package app;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;

public class Main {

  public static void main(String... args) throws Throwable {

    Vertx vertx = Vertx.vertx();
    TcpEventBusBridge bridge = TcpEventBusBridge.create(vertx,
        new BridgeOptions()
            .addInboundPermitted(new PermittedOptions().setAddressRegex("sample.*"))
            .addOutboundPermitted(new PermittedOptions().setAddressRegex("sample.*"))
    );

    vertx.eventBus().consumer("sample.dumb.inbox", message -> {
      JsonObject body = (JsonObject) message.body();
      System.out.println(body.encodePrettily());
    });

    MessageProducer<Object> tickPublisher = vertx.eventBus().publisher("sample.clock.ticks");
    vertx.setPeriodic(1000L, id -> {
      tickPublisher.send(new JsonObject().put("tick", id));
    });

    vertx.eventBus().consumer("sample.echo", message -> {
      JsonObject body = (JsonObject) message.body();
      System.out.println("Echoing: " + body.encodePrettily());
      message.reply(body);
    });

    bridge.listen(7000, result -> {
      if (result.failed()) {
        throw new RuntimeException(result.cause());
      } else {
        System.out.println("TCP Event Bus bridge running on port 7000");
      }
    });
  }
}
