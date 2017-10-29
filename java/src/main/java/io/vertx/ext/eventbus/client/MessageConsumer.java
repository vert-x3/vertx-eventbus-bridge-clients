package io.vertx.ext.eventbus.client;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class MessageConsumer<T> {

  private EventBusClient client;
  private final String address;
  private final MessageHandler<T> handler;
  private final AtomicBoolean registered = new AtomicBoolean(true);

  public MessageConsumer(final EventBusClient client, final String address, final Handler<Message<T>> handler) {
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
    return this.handler;
  }

  public String address() {
    return this.address;
  }

  public void unregister() {
    if (this.registered.compareAndSet(true, false)) {
      this.client.unregister(this.handler, true);
    }
  }
}
