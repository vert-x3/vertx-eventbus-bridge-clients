package io.vertx.ext.eventbus.client;

import com.google.gson.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class TcpBusTest {

  Vertx vertx;

  @Before
  public void before(TestContext ctx) {
    vertx = Vertx.vertx();
    setUpBridge(ctx);
  }

  protected void setUpBridge(TestContext ctx) {
    TcpEventBusBridge bridge = TcpEventBusBridge.create(
        vertx,
        new BridgeOptions()
            .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
            .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*")));

    bridge.listen(7000, ctx.asyncAssertSuccess());
  }

  protected EventBusClient client() {
    return EventBusClient.tcp(7000, "localhost");
  }

  @After
  public void after(TestContext ctx) {
    if (vertx != null) {
      Async async = ctx.async();
      vertx.close(v -> {
        async.complete();
      });
    }
  }

  @Test
  public void testSend(final TestContext ctx) {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      ctx.assertEquals(new io.vertx.core.json.JsonObject().put("message", "hello"), msg.body());
      async.complete();
    });
    EventBusClient client = client();
    JsonObject obj = new JsonObject();
    obj.addProperty("message", "hello");
    client.send("server_addr", obj);
    client.close();
  }

  @Test
  public void testSubscribe(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    final EventBusClient client = client();
    client.consumer("client_addr", new Handler<Message<Object>>() {
      @Override
      public void handle(Message<Object> event) {
        JsonObject body = (JsonObject) event.body();
        ctx.assertEquals("hello", body.get("message").getAsString());
        async.complete();
        client.connect();
      }
    });
    client.send("send_to_client", new JsonObject());
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject().put("message", "hello"));
    });
  }

  @Test
  public void testUnsubscribe(final TestContext ctx) throws Exception {
    Async async = ctx.async();
    EventBusClient client = client();
    AtomicReference<MessageConsumer> consumer = new AtomicReference<>();
    consumer.set(client.consumer("client_addr", new Handler<Message<Object>>() {
      @Override
      public void handle(Message<Object> event) {
        JsonObject body = (JsonObject) event.body();
        ctx.assertEquals("hello", body.get("message").getAsString());
        consumer.get().unregister();
        async.complete();
      }
    }));
    client.send("send_to_client", new JsonObject());
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject().put("message", "hello"));
    });
    Async async2 = ctx.async();
    vertx.eventBus().consumer("send_to_client_fail", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject().put("message", "hello"), ar -> {
        ctx.assertFalse(ar.succeeded());
        ReplyException err = (ReplyException) ar.cause();
        ctx.assertEquals(ReplyFailure.NO_HANDLERS, err.failureType());
        async2.complete();
      });
    });
    async.awaitSuccess(5000000);
    client.send("send_to_client_fail", new JsonObject());
  }

  @Test
  public void testReply(final TestContext ctx) {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new io.vertx.core.json.JsonObject().put("message", "the_response"));
    });
    EventBusClient client = client();
    JsonObject obj = new JsonObject();
    obj.addProperty("message", "hello");
    client.send("server_addr", obj, new Handler<AsyncResult<Message<JsonObject>>>() {
      @Override
      public void handle(AsyncResult<Message<JsonObject>> event) {
        ctx.assertTrue(event.succeeded());
        ctx.assertEquals("the_response", event.result().body().get("message").getAsString());
        async.complete();
      }
    });
//    client.close();
  }

  @Test
  public void testReplyTimeout(final TestContext ctx) {
    final Async async = ctx.async();
    final AtomicBoolean received = new AtomicBoolean();
    vertx.eventBus().consumer("server_addr", msg -> {
      received.set(true);
    });
    EventBusClient client = client();
    JsonObject obj = new JsonObject();
    obj.addProperty("message", "hello");
    client.send("server_addr", obj, new DeliveryOptions().setSendTimeout(100), new Handler<AsyncResult<Message<JsonObject>>>() {
      @Override
      public void handle(AsyncResult<Message<JsonObject>> event) {
        ctx.assertTrue(received.get());
        ctx.assertTrue(event.failed());
        async.complete();
      }
    });
  }
}
