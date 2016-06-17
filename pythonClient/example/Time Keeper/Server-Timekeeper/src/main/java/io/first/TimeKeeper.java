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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
/**
 *
 * @author jay
 */
public class TimeKeeper extends AbstractVerticle{

  public void start(Future<Void> fut){

	TcpEventBusBridge bridge = TcpEventBusBridge.create(
    vertx,
    new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("Date"))
        .addOutboundPermitted(new PermittedOptions().setAddress("Date"))
		.addInboundPermitted(new PermittedOptions().setAddress("Time"))
		.addOutboundPermitted(new PermittedOptions().setAddress("Time"))
		.addInboundPermitted(new PermittedOptions().setAddress("Get"))
        .addOutboundPermitted(new PermittedOptions().setAddress("Get")));

	bridge.listen(7000, res -> {
		if (res.succeeded()) {
		System.out.println("Started");
		} else {
		System.out.println("failed");
		}
	});
	EventBus eb = vertx.eventBus();
	
	eb.consumer("Time", message -> {
		System.out.println("Get time\n: " + message.body());
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println( sdf.format(cal.getTime()) );
		String jsonString = "{\"Time\":\""+sdf.format(cal.getTime())+"\"}";
		JsonObject object = new JsonObject(jsonString);
		message.reply(object);
	});
	
	eb.consumer("Date", message -> {
		System.out.println("Get date\n: " + message.body());
		Date date=new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        System.out.println( sdf.format(date) );
		String jsonString = "{\"Date\":\""+sdf.format(date)+"\"}";
		JsonObject object = new JsonObject(jsonString);
		message.reply(object);
	});
	
	eb.consumer("Get", message -> {
		System.out.println("Get\n: " + message.body());
		//send date
		String str=message.body().toString();
		if(str.indexOf("send date") != -1){
			Date date=new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
			System.out.println( sdf.format(date) );
			String jsonString = "{\"Date\":\""+sdf.format(date)+"\"}";
			JsonObject object = new JsonObject(jsonString);
			message.reply(object);
		}
		//send time
		else if(str.indexOf("send time") != -1){
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			System.out.println( sdf.format(cal.getTime()) );
			String jsonString = "{\"Time\":\""+sdf.format(cal.getTime())+"\"}";
			JsonObject object = new JsonObject(jsonString);
			message.reply(object);
		}
		else{
			System.out.println(str);
		}
	});
	

  }
}
