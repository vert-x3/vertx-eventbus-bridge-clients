package io.vertx.ext.eventbus.client.test;

import io.vertx.ext.eventbus.client.EventBusClient;
import io.vertx.ext.eventbus.client.EventBusClientOptions;
import io.vertx.ext.eventbus.client.json.JacksonCodec;
import io.vertx.ext.unit.TestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketJacksonBusTest extends WebSocketBusTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    WebSocketBusTest.beforeClass();
  }

  @AfterClass
  public static void afterClass() {
    WebSocketBusTest.afterClass();
  }

  @Override
  public void before(TestContext ctx) {
    super.before(ctx);
    baseOptions = new EventBusClientOptions().setPort(7000).setWebSocketPath("/eventbus-test/websocket")
      .setMaxWebSocketFrameSize(1024 * 1024);
  }

  @Override
  protected EventBusClient client(TestContext ctx) {
    ctx.put("codec", new JacksonCodec());
    return EventBusClient.webSocket(baseOptions, new JacksonCodec());
  }
}
