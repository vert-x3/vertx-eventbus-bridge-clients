package io.vertx.ext.eventbus.client;

import com.google.gson.JsonObject;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketBusTest extends TcpBusTest {

  protected void setUpBridge(TestContext ctx) {
    Router router = Router.router(vertx);
    BridgeOptions opts = new BridgeOptions()
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

  // Does not pass for TCP
  @Test
  public void testSendError(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client();
    JsonObject obj = new JsonObject();
    obj.addProperty("message", "hello");
    client.send("server_addr", obj, new Handler<AsyncResult<Message<JsonObject>>>() {
      @Override
      public void handle(AsyncResult<Message<JsonObject>> event) {
        ctx.assertTrue(event.failed());
        async.complete();
      }
    });
  }

  // Does not pass for TCP
  @Test
  public void testReplyFailure(final TestContext ctx) {
    final Async async = ctx.async();
    final AtomicBoolean received = new AtomicBoolean();
    vertx.eventBus().consumer("server_addr", msg -> {
      received.set(true);
      msg.fail(123, "the_message");
    });
    EventBusClient client = client();
    JsonObject obj = new JsonObject();
    obj.addProperty("message", "hello");
    client.send("server_addr", obj, new Handler<AsyncResult<Message<JsonObject>>>() {
      @Override
      public void handle(AsyncResult<Message<JsonObject>> event) {
        ctx.assertTrue(event.failed());
        async.complete();
      }
    });
  }
}
