package io.vertx.ext.eventbus.client.test;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import io.vertx.ext.eventbus.client.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.test.core.SocksProxy;
import org.junit.*;
import org.junit.runner.RunWith;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class TcpBusTest {

  private static SocksProxy socksProxy;
  EventBusClientOptions baseOptions;
  Vertx vertx;

  @BeforeClass
  public static void beforeClass() throws UnknownHostException {
    socksProxy = new SocksProxy("vertx-user");
    socksProxy.start(Vertx.vertx(), v -> {
    });
  }

  @AfterClass
  public static void afterClass() {
    socksProxy.stop();
  }

  @Before
  public void before(TestContext ctx) {
    vertx = Vertx.vertx();
    baseOptions = new EventBusClientOptions().setPort(7000);
    setUpBridges(ctx);
  }

  protected void setUpBridges(TestContext ctx) {

    BridgeOptions options = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));

    TcpEventBusBridge plainBridge = TcpEventBusBridge.create(vertx, options)
      .listen(7000, ctx.asyncAssertSuccess());

    TcpEventBusBridge.create(vertx, options, new NetServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(new JksOptions().setPath("server-keystore.jks").setPassword("wibble")))
      .listen(7001, ctx.asyncAssertSuccess());

    ctx.put("bridge", plainBridge);
  }

  protected void stopBridge(TestContext ctx, Handler<Void> handler) {

    ctx.<TcpEventBusBridge>get("bridge").close(v -> {
      ctx.assertTrue(v.succeeded());
      handler.handle(null);
    });
  }

  protected void startBridge(TestContext ctx, Handler<Void> handler) {

    ctx.<TcpEventBusBridge>get("bridge").listen(7000, v -> {
      ctx.assertTrue(v.succeeded());
      handler.handle(null);
    });
  }

  protected EventBusClient client(TestContext ctx) {
    return EventBusClient.tcp(baseOptions);
  }

  @After
  public void after(TestContext ctx) {
    if (vertx != null) {
      Async async = ctx.async();
      vertx.close(v -> async.complete());
    }
  }

  @Test
  public void testSend(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    vertx.eventBus().consumer("server_addr", msg -> {
      ctx.assertEquals(new JsonObject().put("message", "hello"), msg.body());
      client.close();
      async.complete();
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"));
  }

  @Test
  public void testPublish(final TestContext ctx) {
    int num = 3;
    final Async async = ctx.async(num);
    EventBusClient client = client(ctx);
    for (int i = 0; i < num; i++) {
      vertx.eventBus().consumer("server_addr", msg -> {
        ctx.assertEquals(new JsonObject().put("message", "hello"), msg.body());
        countDownAndCloseClient(async, client);
      });
    }
    client.publish("server_addr", Collections.singletonMap("message", "hello"));
  }

  @Test
  public void testSubscribe(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    final EventBusClient client = client(ctx);
    client.consumer("client_addr", new Handler<Message<Object>>() {
      @Override
      public void handle(Message<Object> event) {
        Map body = (Map) event.body();
        ctx.assertEquals("hello", body.get("message"));
        async.complete();
        client.close();
      }
    });
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject().put("message", "hello"));
    });
    client.send("send_to_client", Collections.emptyMap());
  }

  @Test
  public void testSubscribeSeveralHandlers(final TestContext ctx) throws Exception {
    int num = 3;
    final Async async = ctx.async(num);
    final EventBusClient client = client(ctx);
    for (int i = 0; i < num; i++) {
      client.consumer("client_addr", new Handler<Message<Object>>() {
        @Override
        public void handle(Message<Object> event) {
          Map body = (Map) event.body();
          ctx.assertEquals("hello", body.get("message"));
          countDownAndCloseClient(async, client);
        }
      });
    }
    vertx.eventBus().consumer("publish_to_client", msg -> {
      vertx.eventBus().publish("client_addr", new JsonObject().put("message", "hello"));
    });
    client.send("publish_to_client", Collections.emptyMap());
  }

  @Test
  public void testUnsubscribe(final TestContext ctx) throws Exception {
    Async async = ctx.async();
    EventBusClient client = client(ctx);
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
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject().put("message", "hello"));
    });
    client.send("send_to_client", Collections.emptyMap());
    Async async2 = ctx.async();
    vertx.eventBus().consumer("send_to_client_fail", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject().put("message", "hello"), ar -> {
        ctx.assertFalse(ar.succeeded());
        ReplyException err = (ReplyException) ar.cause();
        ctx.assertEquals(ReplyFailure.NO_HANDLERS, err.failureType());
        client.close();
        async2.complete();
      });
    });
    async.awaitSuccess(5000);
    client.send("send_to_client_fail", Collections.emptyMap());
  }

  @Test
  public void testReply(final TestContext ctx) {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new JsonObject().put("message", "the_response"));
    });
    EventBusClient client = client(ctx);
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.succeeded());
        ctx.assertEquals("the_response", event.result().body().get("message"));
        client.close();
        async.complete();
      }
    });
  }

  @Test
  public void testSendError(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.failed());
        client.close();
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
    EventBusClient client = client(ctx);
    client.send("server_addr", Collections.singletonMap("message", "hello"), new Handler<AsyncResult<Message<Map>>>() {
      @Override
      public void handle(AsyncResult<Message<Map>> event) {
        ctx.assertTrue(event.failed());
        client.close();
        async.complete();
      }
    });
  }

  @Test
  public void testReplyToServer(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    client.consumer("client_addr", msg -> {
      ctx.assertNotNull(msg.replyAddress());
      msg.reply(Collections.singletonMap("message", "bye"));
    });
    vertx.eventBus().consumer("server_addr", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject(), ctx.asyncAssertSuccess(reply -> {
        ctx.assertEquals(new JsonObject().put("message", "bye"), reply.body());
        client.close();
        async.complete();
      }));
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"));
  }

  @Test
  public void testSendHeaders(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    vertx.eventBus().consumer("server_addr", msg -> {
      ctx.assertEquals(1, msg.headers().size());
      ctx.assertEquals("foo_value", msg.headers().get("foo"));
      client.close();
      async.complete();
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"), new DeliveryOptions().addHeader("foo", "foo_value"));
  }

  @Test
  public void testReceiveHeaders(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    client.consumer("client_addr", msg -> {
      ctx.assertEquals(1, msg.headers().size());
      ctx.assertEquals("foo_value", msg.headers().get("foo"));
      client.close();
      async.complete();
    });
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject(), new io.vertx.core.eventbus.DeliveryOptions().addHeader("foo", "foo_value"));
    });
    client.send("send_to_client", Collections.emptyMap());
  }

  @Test
  public void testFailInConsumer(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    RuntimeException failure = new RuntimeException();
    client.exceptionHandler(err -> {
      ctx.assertEquals(failure, err);
      client.close();
      async.complete();
    });
    client.consumer("client_addr", msg -> {
      throw failure;
    });
    vertx.eventBus().consumer("send_to_client", msg -> {
      vertx.eventBus().send("client_addr", new JsonObject());
    });
    client.send("send_to_client", Collections.emptyMap());
  }

  @Test
  public void testFailInReplyMessageHandler(final TestContext ctx) {
    testFailInReplyHandler(ctx, false);
  }

  @Test
  public void testFailInReplyFailureHandler(final TestContext ctx) {
    testFailInReplyHandler(ctx, true);
  }

  private void testFailInReplyHandler(final TestContext ctx, boolean fail) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);
    RuntimeException failure = new RuntimeException();
    client.exceptionHandler(err -> {
      ctx.assertEquals(failure, err);
      client.close();
      async.complete();
    });
    vertx.eventBus().consumer("server_addr", msg -> {
      if (fail) {
        msg.fail(0, "whatever");
      } else {
        msg.reply(new JsonObject());
      }
    });
    client.send("server_addr", Collections.emptyMap(), reply -> {
      throw failure;
    });
  }

  @Test
  public void testConnectionHandler(final TestContext ctx) throws Exception {
    final Async async = ctx.async(3);
    AtomicBoolean connectedHandlerDone = new AtomicBoolean(false);
    EventBusClient client = client(ctx);

    client.connectedHandler(handler -> {

      ctx.assertTrue(client.isConnected());
      connectedHandlerDone.set(true);
      async.countDown();
      handler.handle(null);
    });

    client.closeHandler(v -> {

      ctx.assertFalse(client.isConnected());
      async.countDown();
    });

    client.send("server_addr", Collections.emptyMap(), reply -> {

      client.close();

      if (!connectedHandlerDone.get()) {
        ctx.fail("Messages should only be send after the connectedHandler has called it's handler.");
        return;
      }

      async.countDown();
    });

    async.awaitSuccess(5000);
  }

  @Test
  public void testAutoReconnect(final TestContext ctx) throws Exception {
    final Async async = ctx.async(2);

    EventBusClient client = client(ctx);

    client.connectedHandler(handler -> {

      countDownAndCloseClient(async, client);
      handler.handle(null);
    });

    client.connect();

    sleep(ctx, 3000, "Could not sleep before stopping bridge.");

    this.stopBridge(ctx, v -> {

      sleep(ctx, 2000, "Could not sleep after stopping bridge.");
      this.startBridge(ctx, x -> {
      });
    });
  }

  @Test
  public void testSendDefaultOptionsHeaders(final TestContext ctx) throws Exception {
    final Async async = ctx.async(3);
    EventBusClient client = client(ctx);

    vertx.eventBus().consumer("server_addr", msg -> {
      String token = msg.headers().get("token");
      if (token == null) {
        msg.reply(new JsonObject().putNull("token"));
      } else {
        msg.reply(new JsonObject().put("token", token));
      }
    });

    client.<Map>send("server_addr", Collections.emptyMap(), reply -> {
      ctx.assertTrue(reply.succeeded());
      ctx.assertNull(reply.result().body().get("token"));
      countDownAndCloseClient(async, client);
    });

    client.setDefaultDeliveryOptions(new DeliveryOptions().addHeader("token", "hello world"));

    client.<Map>send("server_addr", Collections.emptyMap(), reply -> {
      ctx.assertTrue(reply.succeeded());
      ctx.assertEquals("hello world", reply.result().body().get("token"));
      countDownAndCloseClient(async, client);
    });

    client.<Map>send("server_addr", Collections.emptyMap(), new DeliveryOptions().addHeader("token", "hello mars"), reply -> {
      ctx.assertTrue(reply.succeeded());
      ctx.assertEquals("hello mars", reply.result().body().get("token"));
      countDownAndCloseClient(async, client);
    });

    async.awaitSuccess(5000);
  }

  @Test
  public void testIdleTimeout(final TestContext ctx) throws Exception {
    final Async async = ctx.async(5);

    baseOptions
      .setIdleTimeout(100)
      .setAutoReconnectInterval(0);

    EventBusClient client = client(ctx);

    client.connectedHandler(event -> {
      countDownAndCloseClient(async, client);
    });

    client.connect();

    async.awaitSuccess(3000);
  }

  @Test
  public void testSslTrustAll(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setTrustAll(true)
      .setAutoReconnect(false);

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testSslTrustException(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setAutoReconnect(false);

    EventBusClient client = client(ctx);

    client.connectedHandler(event -> {
      client.close();
      ctx.fail("Should not connect.");
    });

    client.closeHandler(event -> {
      client.close();
      ctx.fail("Should not disconnect.");
    });

    client.exceptionHandler(throwable -> {
      async.complete();
    });

    client.connect();
  }

  @Test
  public void testSslJksTruststore(final TestContext ctx) throws Exception {
    final Async async = ctx.async();

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setAutoReconnect(false)
      .setTrustStorePath("server-keystore.jks")
      .setTrustStorePassword("wibble");

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testSslPemTruststore(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setAutoReconnect(false)
      .setTrustStoreType("pem")
      .setTrustStorePath("server-keystore-nopass.pem");

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testSslPfxTruststore(final TestContext ctx) throws Exception {
    final Async async = ctx.async();

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setAutoReconnect(false)
      .setTrustStorePath("server-keystore.pfx")
      .setTrustStorePassword("wibble")
      .setTrustStoreType("pfx");

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testVerifyHostsFailure(final TestContext ctx) throws Exception {
    final Async async = ctx.async();

    baseOptions
      .setHost("127.0.0.1")
      .setPort(7001)
      .setSsl(true)
      .setAutoReconnect(false)
      .setTrustStorePath("server-keystore.pfx")
      .setTrustStorePassword("wibble")
      .setTrustStoreType("pfx");

    EventBusClient client = client(ctx);

    client.connectedHandler(event -> {
      client.close();
      ctx.fail("Should not connect.");
    });

    client.exceptionHandler(event -> {
      async.complete();
    });

    client.connect();
  }

  @Test
  public void testPing(final TestContext ctx) throws Exception {
    final Async async = ctx.async();
    vertx.eventBus().consumer("server_addr", msg -> {
      async.complete();
    });
    EventBusClient client = client(ctx);
    AtomicBoolean closed = new AtomicBoolean();
    client.closeHandler(v -> {
      System.out.println("closed");
      closed.set(true);
    });
    client.send("server_addr", Collections.singletonMap("message", "hello"));
    sleep(ctx, 6000, "Could not sleep while testing ping.");
    ctx.assertFalse(closed.get());
    client.close();
  }

  @Test
  public void testProxySocks5(final TestContext ctx) {
    final Async async = ctx.async();

    // VertX SocksProxy only supports connecting to hostnames, not IPv4/6
    baseOptions
      .setPort(7000)
      .setAutoReconnect(false)
      .setProxyType(ProxyType.SOCKS5)
      .setProxyHost("localhost")
      .setProxyPort(11080)
      .setProxyUsername("vertx-user")
      .setProxyPassword("vertx-user");

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testProxySocks5SslTrustAll(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions
      .setPort(7001)
      .setSsl(true)
      .setTrustAll(true)
      .setAutoReconnect(false)
      .setProxyType(ProxyType.SOCKS5)
      .setProxyHost("localhost")
      .setProxyPort(11080)
      .setProxyUsername("vertx-user")
      .setProxyPassword("vertx-user");

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testProxySocks5UserFailure(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions
      .setPort(7000)
      .setAutoReconnect(false)
      .setProxyType(ProxyType.SOCKS5)
      .setProxyHost("localhost")
      .setProxyPort(11080)
      .setProxyUsername("vertx-user2")
      .setProxyPassword("vertx-user");

    EventBusClient client = client(ctx);

    client.connectedHandler(event -> ctx.fail("Should not connect."));
    client.exceptionHandler(event -> {
      async.complete();
      client.close();
    });
    client.connect();
  }

  @Test
  public void testProxySocks5Failure(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions
      .setPort(7000)
      .setAutoReconnect(false)
      .setProxyType(ProxyType.SOCKS5)
      .setProxyHost("localhost")
      .setProxyPort(11081)
      .setProxyUsername("vertx-user")
      .setProxyPassword("vertx-user");
    EventBusClient client = client(ctx);

    performHelloWorldFailure(ctx, async, client);
  }

  protected synchronized void countDownAndCloseClient(Async async, EventBusClient client) {
    if (async.count() == 1) {
      client.close();
    }

    async.countDown();
  }

  protected void performHelloWorld(final TestContext ctx, final Async async, final EventBusClient client) {
    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new JsonObject().put("message", "hello world"));
    });
    client.exceptionHandler(event -> ctx.fail(new Exception("Should not end in exception.", event)));
    client.<Map>send("server_addr", Collections.emptyMap(), reply -> {
      ctx.assertTrue(reply.succeeded());
      ctx.assertEquals("hello world", reply.result().body().get("message"));
      client.close();
      async.complete();
    });
  }

  protected void performHelloWorldFailure(final TestContext ctx, final Async async, final EventBusClient client) {
    client.connectedHandler(event -> {
      client.close();
      ctx.fail("Should not connect.");
    });
    client.exceptionHandler(event -> async.complete());
    client.connect();
  }

  protected void sleep(final TestContext ctx, int duration, String error) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      ctx.fail(error);
    }
  }

  // TODO: add test for EventBusClientOptions.connectTimeout
}
