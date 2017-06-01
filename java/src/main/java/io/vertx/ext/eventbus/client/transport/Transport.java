package io.vertx.ext.eventbus.client.transport;

import io.netty.channel.ChannelInitializer;
import io.vertx.ext.eventbus.client.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class Transport extends ChannelInitializer {

  protected Handler<Void> connectedHandler;
  protected Handler<String> messageHandler;

  public void connectedHandler(Handler<Void> connectedHandler) {
    this.connectedHandler = connectedHandler;
  }

  public void messageHandler(Handler<String> messageHandler) {
    this.messageHandler = messageHandler;
  }

  public abstract void send(String message);

}
