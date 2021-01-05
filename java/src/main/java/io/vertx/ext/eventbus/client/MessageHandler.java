package io.vertx.ext.eventbus.client;

import java.util.concurrent.ScheduledFuture;

/**
 * A registered message handler that will be called when a message is received from event bus.
 * <p>
 * It is possible to have multiple {@code MessageHandler} instances on the same address, the handlers will be called
 * when a message is received from event bus, the order of the handlers to be invoked is the register order.
 * <p>
 * Only one Vertx EventBus Consumer instance gets registered in the server side for the same address, other
 * {@code MessageHandler} instances are handled in the client side.
 * <p>
 * This is not intended to be used by application.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
abstract class MessageHandler<T> {

  private ScheduledFuture<?> timeout;

  /**
   * Address on which this handler observes.
   *
   * @return the address that this handler observes.
   */
  public abstract String address();

  /**
   * Handles the {@link Message}.
   *
   * @param msg the {@link Message} received from event bus.
   */
  public abstract void handleMessage(Message<T> msg);

  /**
   * Handles the Throwable.
   *
   * @param err the Throwable.
   */
  public void handleError(Throwable err) {
  }

  /**
   * Sets the timeout runnable when client does not get response from server.
   *
   * @param timeout the runnable to execute when timeout.
   */
  void setTimeout(ScheduledFuture<?> timeout) {
    this.timeout = timeout;
  }

  /**
   * Cancels the timeout runnable.
   */
  void cancelTimeout() {
    if (this.timeout != null && !this.timeout.isCancelled()) {
      this.timeout.cancel(false);
    }
  }
}
