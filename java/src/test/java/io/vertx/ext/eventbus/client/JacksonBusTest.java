package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonBusTest extends WebSocketBusTest {

  protected EventBusClient client() {
    return EventBusClient.websocket(8080, "localhost", new JacksonCodec());
  }
}
