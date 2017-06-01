package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DeliveryOptions {

  /**
   * The default send timeout.
   */
  public static final long DEFAULT_TIMEOUT = 30 * 1000;

  private long timeout = DEFAULT_TIMEOUT;

  /**
   * Get the send timeout.
   * <p>
   * When sending a message with a response handler a send timeout can be provided. If no response is received
   * within the timeout the handler will be called with a failure.
   *
   * @return  the value of send timeout
   */
  public long getSendTimeout() {
    return timeout;
  }

  /**
   * Set the send timeout.
   *
   * @param timeout  the timeout value, in ms.
   * @return  a reference to this, so the API can be used fluently
   */
  public DeliveryOptions setSendTimeout(long timeout) {
    if (timeout < 1) {
      throw new IllegalStateException("sendTimeout must be >= 1");
    }
    this.timeout = timeout;
    return this;
  }
}
