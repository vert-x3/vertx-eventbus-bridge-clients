package io.vertx.ext.eventbus.client;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketBusTest extends TcpBusTest {

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
}
