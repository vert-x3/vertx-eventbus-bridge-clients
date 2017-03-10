package test;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;

public class TcpEventBusBridgeEchoServer {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.eventBus().consumer("hello", (Message<JsonObject> msg) -> {
      msg.reply(new JsonObject().put("value", "Hello " + msg.body().getString("value")));
    });

    vertx.eventBus().consumer("echo",
        (Message<JsonObject> msg) -> msg.reply(msg.body()));

    TcpEventBusBridge bridge = TcpEventBusBridge.create(
        vertx,
        new BridgeOptions()
            .addInboundPermitted(new PermittedOptions().setAddress("hello"))
            .addInboundPermitted(new PermittedOptions().setAddress("echo"))
            .addOutboundPermitted(new PermittedOptions().setAddress("echo")));

    bridge.listen(7000, res -> System.out.println("Ready"));
  }
}
