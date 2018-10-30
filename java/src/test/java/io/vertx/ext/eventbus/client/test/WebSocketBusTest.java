package io.vertx.ext.eventbus.client.test;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.client.EventBusClient;
import io.vertx.ext.eventbus.client.EventBusClientOptions;
import io.vertx.ext.eventbus.client.Handler;
import io.vertx.ext.eventbus.client.ProxyType;
import io.vertx.ext.eventbus.client.json.GsonCodec;
import io.vertx.ext.eventbus.client.json.JsonCodec;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.junit.*;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketBusTest extends TcpBusTest {

  private static HttpProxyServer httpProxy;
  private static int MAX_WEBSOCKET_FRAME_SIZE = 1024 * 1024;

  @BeforeClass
  public static void beforeClass() throws UnknownHostException {
    TcpBusTest.beforeClass();
    httpProxy = DefaultHttpProxyServer.bootstrap().withPort(8000).withAllowLocalOnly(true).start();
  }

  @AfterClass
  public static void afterClass() {
    TcpBusTest.afterClass();
    httpProxy.stop();
  }

  @Override
  public void before(TestContext ctx) {
    super.before(ctx);
    baseOptions = new EventBusClientOptions()
      .setPort(7000)
      .setWebsocketPath("/eventbus-test/websocket")
      .setWebsocketMaxWebsocketFrameSize(MAX_WEBSOCKET_FRAME_SIZE);
  }

  @Override
  protected void setUpBridges(TestContext ctx) {
    Router router = Router.router(vertx);
    BridgeOptions opts = new BridgeOptions()
      .setPingTimeout(15000)
      .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*"));
    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
    router.route("/eventbus-test/*").handler(ebHandler);
    HttpServer server = vertx.createHttpServer(new HttpServerOptions().setMaxWebsocketFrameSize(MAX_WEBSOCKET_FRAME_SIZE).setMaxWebsocketMessageSize(MAX_WEBSOCKET_FRAME_SIZE))
      .requestHandler(router::accept)
      .listen(7000, ctx.asyncAssertSuccess());

    vertx.createHttpServer(new HttpServerOptions().setMaxWebsocketFrameSize(MAX_WEBSOCKET_FRAME_SIZE).setMaxWebsocketMessageSize(MAX_WEBSOCKET_FRAME_SIZE).setSsl(true).setKeyStoreOptions(
      new JksOptions().setPath("server-keystore.jks").setPassword("wibble")
    ))
      .requestHandler(router::accept)
      .listen(7001, ctx.asyncAssertSuccess());

    ctx.put("bridge", server);
  }

  @Override
  protected void stopBridge(TestContext ctx, Handler<Void> handler) {

    ctx.<HttpServer>get("bridge").close(v -> {
      ctx.assertTrue(v.succeeded());
      handler.handle(null);
    });
  }

  @Override
  protected void startBridge(TestContext ctx, Handler<Void> handler) {

    ctx.<HttpServer>get("bridge").listen(7000, v -> {
      ctx.assertTrue(v.succeeded());
      handler.handle(null);
    });
  }

  @Override
  protected EventBusClient client(TestContext ctx) {
    ctx.put("codec", new GsonCodec());
    return EventBusClient.websocket(baseOptions);
  }

  // This test is blocked by netty issue https://github.com/netty/netty/issues/5070
  /*
  @Test
  public void testProxyHttpSsl(final TestContext ctx) {
    final Async async = ctx.async();
    setUpProxy();
    EventBusClient client = client(ctx);

    ctx.<EventBusClientOptions>get("clientOptions").setPort(7001).setSsl(true).setTrustAll(true).setVerifyHost(false).setAutoReconnect(false)
                                                   .setProxyOptions(new ProxyOptions(ProxyType.HTTP, "localhost", 8000));

    performHelloWorld(ctx, async, client);
  }*/

  @Test
  public void testProxyHttp(final TestContext ctx) {
    final Async async = ctx.async();

    baseOptions.setPort(7000).setAutoReconnect(false)
      .setProxyType(ProxyType.HTTP).setProxyHost("localhost").setProxyPort(8000);

    EventBusClient client = client(ctx);

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testProxyHttpFailure(final TestContext ctx) {
    final Async async = ctx.async();

     baseOptions
       .setPort(7000)
       .setAutoReconnect(false)
       .setProxyType(ProxyType.HTTP)
       .setProxyHost("localhost")
       .setProxyPort(8100);

    EventBusClient client = client(ctx);

    performHelloWorldFailure(ctx, async, client);
  }

  @Test
  public void testMaxWebSocketFrameSend(final TestContext ctx) throws Exception {

    final Async async = ctx.async(2);

    baseOptions.setPort(7000).setAutoReconnect(false);

    EventBusClient client = client(ctx);

    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new JsonObject());
    });

    client.send("server_addr", getStringForJsonObjectTargetByteSize(ctx, "server_addr", 128), response -> {
      ctx.assertTrue(response.succeeded(), "Message within MaxWebSocketFrameSize limit should succeed.");
      countDownAndCloseClient(async, client);
    });
    client.send("server_addr", getStringForJsonObjectTargetByteSize(ctx, "server_addr", MAX_WEBSOCKET_FRAME_SIZE - 8), response -> {
      ctx.assertTrue(response.succeeded(), "Message within MaxWebSocketFrameSize limit should succeed.");
      countDownAndCloseClient(async, client);
    });
  }

  @Test
  public void testMaxWebSocketFrameSizeSendFail(final TestContext ctx) throws Exception {

    final Async async = ctx.async();

    baseOptions.setPort(7000).setAutoReconnect(false);

    EventBusClient client = client(ctx);

    vertx.eventBus().consumer("server_addr", msg -> {
      msg.reply(new JsonObject());
    });

    client.exceptionHandler(event -> {
      // Is not being fired, as we don't have any indication of an error
    });

    client.closeHandler(event -> {
      async.complete();
    });

    client.connectedHandler(event -> {

      client.send("server_addr", getStringForJsonObjectTargetByteSize(ctx, "server_addr", MAX_WEBSOCKET_FRAME_SIZE + 8), response -> {
        // This will come after 30s, when the request times out, as the SockJS server just drops the connection instead of sending a proper error response
        ctx.assertFalse(response.succeeded(), "Should not be able to send more than MAX_WEBSOCKET_FRAME_SIZE");
      });
    });

    client.connect();
  }

  private String getStringForJsonObjectTargetByteSize(TestContext ctx, String address, int numberOfBytes) {

    String replyAddress = UUID.randomUUID().toString();
    int envelopeLength = ctx.<JsonCodec>get("codec").encode(this.getMessageEnvelope(address, replyAddress, "")).getBytes(StandardCharsets.UTF_8).length;

    String body = getStringWithSize(numberOfBytes - envelopeLength);
    Map<String, Object> currentCandidate = this.getMessageEnvelope("server_addr", replyAddress, body);
    int currentCandidateLength = ctx.<JsonCodec>get("codec").encode(currentCandidate).getBytes(StandardCharsets.UTF_8).length;

    ctx.assertEquals(numberOfBytes, currentCandidateLength, "Could not create string with target byte size.");

    return body;
  }

  private Map<String, Object> getMessageEnvelope(String address, String replyAddress, Object body) {

    Map<String, Object> obj = new HashMap<>();
    obj.put("type", "send");
    obj.put("address", address);
    obj.put("replyAddress", replyAddress);
    obj.put("body", body);
    return obj;
  }

  private String getStringWithSize(int numberOfBytes) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numberOfBytes; ++i) {
      builder.append("x");
    }
    return builder.toString();
  }

  @Override
  public void testSslTrustException(TestContext ctx) {
    super.testSslTrustException(ctx);
  }
}
