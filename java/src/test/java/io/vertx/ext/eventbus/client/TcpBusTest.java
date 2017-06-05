package io.vertx.ext.eventbus.client;

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

import java.util.Collections;
import java.util.Map;
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
    client.send("server_addr", Collections.singletonMap("message", "hello"));
    client.close();
  }

  @Test
  public void testPublish(final TestContext ctx) {
    int num = 3;
    final Async async = ctx.async(num);
    for (int i = 0;i < num;i++) {
      vertx.eventBus().consumer("server_addr", msg -> {
        ctx.assertEquals(new io.vertx.core.json.JsonObject().put("message", "hello"), msg.body());
        async.countDown();
      });
    }
    EventBusClient client = client();
    client.publish("server_addr", Collections.singletonMap("message", "hello"));
    client.close();
  }

  @Test
  public void testSubscribeToSend(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    final EventBusClient client = client();
    client.consumer("client_addr", new Handler<Message<Object>>() {
      @Override
      public void handle(Message<Object> event) {
        Map body = (Map) event.body();
        ctx.assertEquals("hello", body.get("message"));
        async.complete();
        client.connect();
      }
    });
    client.send("send_to_client", Collections.emptyMap());
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject().put("message", "hello"));
    });
  }

  @Test
  public void testSubscribeToPublish(final TestContext ctx) throws Exception {
    int num = 3;
    final Async async = ctx.async(num);
    final EventBusClient client = client();
    for (int i = 0;i < num;i++) {
      client.consumer("client_addr", new Handler<Message<Object>>() {
        @Override
        public void handle(Message<Object> event) {
          Map body = (Map) event.body();
          ctx.assertEquals("hello", body.get("message"));
          async.countDown();
          client.connect();
        }
      });
    }
    client.send("publish_to_client", Collections.emptyMap());
    vertx.eventBus().consumer("publish_to_client", msg -> {
      vertx.eventBus().publish("client_addr", new io.vertx.core.json.JsonObject().put("message", "hello"));
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
        Map body = (Map) event.body();
        ctx.assertEquals("hello", body.get("message"));
        consumer.get().unregister();
        async.complete();
      }
    }));
    client.send("send_to_client", Collections.emptyMap());
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
    client.send("send_to_client_fail", Collections.emptyMap());
  }

  @Test
  public void testReply(final TestContext ctx) {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new io.vertx.core.json.JsonObject().put("message", "the_response"));
    });
    EventBusClient client = client();
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.succeeded());
        ctx.assertEquals("the_response", event.result().body().get("message"));
        async.complete();
      }
    });
//    client.close();
  }

  @Test
  public void testSendError(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client();
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.failed());
        async.complete();
      }
    });
  }

  @Test
  public void testReplyFailure(final TestContext ctx) {
    final Async async = ctx.async();
    final AtomicBoolean received = new AtomicBoolean();
    vertx.eventBus().consumer("server_addr", msg -> {
      received.set(true);
      msg.fail(123, "the_message");
    });
    EventBusClient client = client();
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.failed());
        async.complete();
      }
    });
  }

  @Test
  public void testReplyToServer(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client();
    client.consumer("client_addr", msg -> {
      ctx.assertNotNull(msg.replyAddress());
      msg.reply(Collections.singletonMap("message", "bye"));
    });
    vertx.eventBus().consumer("server_addr", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject(), ctx.asyncAssertSuccess(reply -> {
        ctx.assertEquals(new io.vertx.core.json.JsonObject().put("message", "bye"), reply.body());
        async.complete();
      }));
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"));
  }

  @Test
  public void testSendHeaders(final TestContext ctx) {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      ctx.assertEquals(1, msg.headers().size());
      ctx.assertEquals("foo_value", msg.headers().get("foo"));
      async.complete();
    });
    EventBusClient client = client();
    client.send("server_addr", Collections.singletonMap("message", "hello"), new DeliveryOptions().addHeader("foo", "foo_value"));
  }

  @Test
  public void testReceiveHeaders(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client();
    client.consumer("client_addr", msg -> {
      ctx.assertEquals(1, msg.headers().size());
      ctx.assertEquals("foo_value", msg.headers().get("foo"));
      async.complete();
    });
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new io.vertx.core.json.JsonObject(), new io.vertx.core.eventbus.DeliveryOptions().addHeader("foo", "foo_value"));
    });
    client.send("send_to_client", Collections.emptyMap());
  }
}
