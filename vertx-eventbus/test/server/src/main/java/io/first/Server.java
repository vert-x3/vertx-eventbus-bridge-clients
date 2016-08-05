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
 * @author Jayamine Alupotha
 */
public class Server extends AbstractVerticle{

  public void start(Future<Void> fut){

	TcpEventBusBridge bridge = TcpEventBusBridge.create(
    vertx,
    new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("pcs.status"))
        .addOutboundPermitted(new PermittedOptions().setAddress("pcs.status")));
		
	bridge.listen(7000, res -> {
		if (res.succeeded()) {
		System.out.println("request success");
		} else {
		System.out.println("request failed");
		}
	});
	
	EventBus eb = vertx.eventBus();
	
	eb.consumer("pcs.status", message -> {
		System.out.println("I have received a message for pcs.status: " + message.body());
		System.out.println("headers:"+message.headers());
		String jsonString = "{\"message\":\"command accepted\"}";
		JsonObject object = new JsonObject(jsonString);
		message.reply(object);
	});
	
  }
}
