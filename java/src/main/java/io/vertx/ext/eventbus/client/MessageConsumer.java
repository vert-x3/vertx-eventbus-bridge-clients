package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class MessageConsumer<T> {

  private final EventBusClient client;
  private final String address;
  private final MessageHandler<T> handler;

  public MessageConsumer(EventBusClient client, final String address, final Handler<Message<T>> handler) {
    this.client = client;
    this.address = address;
    this.handler = new MessageHandler<T>() {
      @Override
      public String address() {
        return address;
      }
      @Override
      public void handleMessage(Message<T> msg) {
        handler.handle(msg);
      }
      @Override
      public void handleError(Throwable err) {
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
    client.unregister(handler);
  }
}
