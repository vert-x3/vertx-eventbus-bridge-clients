package io.vertx.ext.eventbus.client;

import java.util.concurrent.ScheduledFuture;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
abstract class MessageHandler<T> {

  private ScheduledFuture<?> timeout;

  public abstract String address();

  public abstract void handleMessage(Message<T> msg);

  public void handleError(Throwable err) {
  }

  void setTimeout(ScheduledFuture<?> timeout) {
    this.timeout = timeout;
  }

  void cancelTimeout() {
    if (this.timeout != null && !this.timeout.isCancelled()) {
      this.timeout.cancel(false);
    }
  }
}
