package io.vertx.ext.eventbus.client;

import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketBusTest extends TcpBusTest {

  protected void setUpBridge(TestContext ctx) {
    Router router = Router.router(vertx);
    BridgeOptions opts = new BridgeOptions()
        .setPingTimeout(300)
        .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
        .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
    router.route("/eventbus/*").handler(ebHandler);
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(router::accept);
    server.listen(8080, ctx.asyncAssertSuccess());
  }

  protected EventBusClient client() {
    return EventBusClient.websocket(8080, "localhost");
  }

  // TCP does not use PING
  @Test
  public void testPing(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      async.complete();
    });
    EventBusClient client = client();
    AtomicBoolean closed = new AtomicBoolean();
    client.closeHandler(v -> {
      System.out.println("closed");
      closed.set(true);
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"));
    Thread.sleep(1000);
    ctx.assertFalse(closed.get());
  }
}
