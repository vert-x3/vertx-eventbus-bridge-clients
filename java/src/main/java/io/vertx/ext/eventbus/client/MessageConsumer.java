package io.vertx.ext.eventbus.client;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class MessageConsumer<T> {

  private final EventBusClient client;
  private final String address;
  private final MessageHandler<T> handler;
  private final AtomicBoolean registered = new AtomicBoolean(true);

  public MessageConsumer(EventBusClient client, final String address, final Handler<Message<T>> handler) {
    this.client = client;
    this.address = address;
    this.handler = new MessageHandler<T>() {
      @Override
      public String address() { return address; };
      @Override
      public void handleMessage(Message<T> msg) {
        handler.handle(msg);
      }
    };
  }

  MessageHandler handler() {
    return handler;
  }

  public String address() {
    return address;
  }

  public void unregister() {
    if (registered.compareAndSet(true, false)) {
      client.unregister(handler);
    }
  }
}
