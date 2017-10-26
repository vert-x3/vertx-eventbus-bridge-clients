package io.vertx.ext.eventbus.client;

import io.vertx.ext.eventbus.client.json.JacksonCodec;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;
import io.vertx.ext.unit.TestContext;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketJacksonBusTest extends WebSocketBusTest {

  @Override
  protected EventBusClient client(TestContext ctx) {
    EventBusClientOptions options = new EventBusClientOptions().setPort(7000)
                                                               .setTransportOptions(new WebSocketTransportOptions().setPath("/eventbus-test/websocket")
                                                                                                                   .setMaxWebsocketFrameSize(1024 * 1024));
    ctx.put("clientOptions", options);
    ctx.put("codec", new JacksonCodec());
    return EventBusClient.websocket(options, new JacksonCodec());
  }
}
