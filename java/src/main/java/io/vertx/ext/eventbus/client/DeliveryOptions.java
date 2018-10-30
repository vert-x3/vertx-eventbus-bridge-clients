package io.vertx.ext.eventbus.client;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DeliveryOptions {

  /**
   * The default send timeout.
   */
  public static final long DEFAULT_TIMEOUT = 30 * 1000;

  private long timeout = DEFAULT_TIMEOUT;
  private Map<String, String> headers;

  /**
   * Get the send timeout.
   * <p>
   * When sending a message with a response handler a send timeout can be provided. If no response is received
   * within the timeout the handler will be called with a failure.
   *
   * @return the value of send timeout
   */
  public long getSendTimeout() {
    return timeout;
  }

  /**
   * Set the send timeout.
   *
   * @param timeout the timeout value, in ms.
   * @return a reference to this, so the API can be used fluently
   */
  public DeliveryOptions setSendTimeout(long timeout) {
    if (timeout < 1) {
      throw new IllegalStateException("sendTimeout must be >= 1");
    }
    this.timeout = timeout;
    return this;
  }

  /**
   * Adds a header to be send with each request.
   *
   * @param key   the header key.
   * @param value the header value.
   * @return a reference to this, so the API can be used fluently
   */
  public DeliveryOptions addHeader(String key, String value) {
    if (headers == null) {
      headers = new HashMap<>();
    }
    headers.put(key, value);
    return this;
  }

  Map<String, String> getHeaders() {
    return headers;
  }
}
