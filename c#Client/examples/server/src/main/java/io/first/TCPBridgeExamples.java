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
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class TCPBridgeExamples extends AbstractVerticle{

  public void start(Future<Void> fut){
	System.out.println("\nstep1\n");
	TcpEventBusBridge bridge = TcpEventBusBridge.create(
    vertx,
    new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("pcs.status"))
        .addOutboundPermitted(new PermittedOptions().setAddress("pcs.status"))
		.addInboundPermitted(new PermittedOptions().setAddress("pcs.status.c"))
		.addOutboundPermitted(new PermittedOptions().setAddress("pcs.status.c")));
	System.out.println("\nstep2\n");
	bridge.listen(7000, res -> {
		System.out.println("Started");
		if (res.succeeded()) {
		System.out.println("request success");
		} else {
		System.out.println("request failed");
		}
	});
	System.out.println("\nstep3\n");
	EventBus eb = vertx.eventBus();
	System.out.println("\nstep4\n");
	eb.consumer("pcs.status", message -> {
		System.out.println("I have received a message for pcs.status: " + message.body());
		System.out.println("headers:"+message.headers());
		String jsonString = "{\"message\":\"command accepted\"}";
		JsonObject object = new JsonObject(jsonString);
		message.reply(object);
	});
	
	
	
	//consumer is a function in eventbus
    /*MessageConsumer<JsonObject> consumer = eb.consumer("pcs.status");
	consumer.handler(message -> {
		System.out.println("I have received a message");
		System.out.println("headers: "+message.headers().toString());
		System.out.println("Body   : "+message.body());
		message.reply("i got it");
		System.out.println("ok");
	});
	
	MessageConsumer<JsonObject> consumer1 = eb.consumer("pcs.status.permit");
	consumer1.handler(message -> {
		System.out.println("I have received a message for permit");
		System.out.println("headers: "+message.headers().toString());
		System.out.println("Body   : "+message.body());
		message.reply("i got it");
		System.out.println("ok");
	});
	
	
	consumer.completionHandler(res -> {
	if (res.succeeded()) {
		System.out.println("The handler registration has reached all nodes");
	} else {
		System.out.println("Registration failed!");
	}
	});*/
  }
}
