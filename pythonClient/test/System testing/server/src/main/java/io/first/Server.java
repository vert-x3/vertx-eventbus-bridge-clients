package io.first;

import io.vertx.core.*;
import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Handler;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.EventBus;
/**
 *
 * @author jay
 */
public class Server extends AbstractVerticle{

  public void start(Future<Void> fut){

	TcpEventBusBridge bridge = TcpEventBusBridge.create(
    vertx,
    new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("add"))
        .addOutboundPermitted(new PermittedOptions().setAddress("add")));

	bridge.listen(7000, res -> {
		if (res.succeeded()) {
		System.out.println("Started");
		} else {
		System.out.println("failed");
		}
	});
	EventBus eb = vertx.eventBus();
	
	MessageConsumer< JsonObject > consumer=eb.consumer("add", message -> {
		System.out.println("Message body: " + message.body());
		String jsonString = "{\"result\":\"4\"}";
		JsonObject object = new JsonObject(jsonString);
		message.reply(object);
	});
	
	
  }
}
