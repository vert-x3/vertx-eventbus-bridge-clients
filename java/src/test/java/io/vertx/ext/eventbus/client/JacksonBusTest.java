package io.vertx.ext.eventbus.client;

import io.vertx.ext.eventbus.client.json.JacksonCodec;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonBusTest extends WebSocketBusTest {

  protected EventBusClient client() {
    return EventBusClient.websocket(new EventBusClientOptions().setPort(7000), new JacksonCodec());
  }
}
