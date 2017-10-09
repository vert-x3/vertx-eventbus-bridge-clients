package io.vertx.ext.eventbus.client;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.eventbus.client.logging.LoggerFactory;
import io.vertx.ext.eventbus.client.options.ProxyOptions;
import io.vertx.ext.eventbus.client.options.ProxyType;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.junit.*;
import org.littleshoot.proxy.ActivityTracker;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.FullFlowContext;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketBusTest extends TcpBusTest {

  static HttpProxyServer proxy;

  @BeforeClass
  public static void beforeClass(TestContext ctx) {
    proxy = DefaultHttpProxyServer.bootstrap().withPort(8000).withAllowLocalOnly(true).start();
  }

  @AfterClass
  public static void afterClass(TestContext ctx) {
    proxy.stop();
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
    HttpServer server = vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(7000, ctx.asyncAssertSuccess());

    vertx.createHttpServer(new HttpServerOptions().setSsl(true).setKeyStoreOptions(
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
    EventBusClientOptions options = new EventBusClientOptions().setPort(7000)
                                                               .setWebSocketTransportOptions(new WebSocketTransportOptions().setPath("/eventbus-test/websocket")
                                                                                                                            .setMaxWebsocketFrameSize(1024 * 1024));
    ctx.put("clientOptions", options);
    return EventBusClient.websocket(options);
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
    EventBusClient client = client(ctx);

    ctx.<EventBusClientOptions>get("clientOptions").setPort(7000).setAutoReconnect(false)
      .setProxyOptions(new ProxyOptions(ProxyType.HTTP, "localhost", 8000));

    performHelloWorld(ctx, async, client);
  }

  @Test
  public void testProxyHttpFailure(final TestContext ctx) {
    final Async async = ctx.async();
    EventBusClient client = client(ctx);

    ctx.<EventBusClientOptions>get("clientOptions").setPort(7000).setAutoReconnect(false)
                                                   .setProxyOptions(new ProxyOptions(ProxyType.HTTP, "localhost", 8001));

    client.connectedHandler(event -> {
      client.close();
      ctx.fail("Should not connect.");
    });

    client.exceptionHandler(event -> {
      async.complete();
    });

    client.connect();
  }
}
