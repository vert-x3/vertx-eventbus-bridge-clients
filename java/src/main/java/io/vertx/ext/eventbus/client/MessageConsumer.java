package io.vertx.ext.eventbus.client;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An event bus message consumer represents a registered {@link Handler} on the specified {@code address}.
 * <p>
 * It is possible to have multiple {@code MessageConsumer} instances on the same address, the handlers will be called
 * when a message is received from event bus, the order of the handlers to be invoked is the register order.
 * <p>
 * The consumer is unregistered from the event bus using the {@link #unregister()} method.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class MessageConsumer<T> {

  private EventBusClient client;
  final String address;
  final MessageHandler<T> handler;
  private final AtomicBoolean registered = new AtomicBoolean(true);

  /**
   * Constructor of the MessageConsumer.
   *
   * @param client the {@link EventBusClient} used to unregister the handler from event bus.
   * @param address the address to monitor the messages from event bus.
   * @param handler the handler which will be called when a message is received from event bus.
   */
  MessageConsumer(final EventBusClient client, final String address, final Handler<Message<T>> handler) {
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
    };
  }

  /**
   * Unregister the {@code handler} from the event bus.
   * <p>
   * The handler is only unregistered once, multiple invokes to this method do nothing.
   */
  public void unregister() {
    if (this.registered.compareAndSet(true, false)) {
      this.client.unregister(this.handler, true);
    }
  }
}
