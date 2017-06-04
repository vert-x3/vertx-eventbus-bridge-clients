package io.vertx.ext.eventbus.client;

import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Message<T> {

  private final String address;
  private final Map<String, String> headers;
  private final T body;

  public Message(String address, Map<String, String> headers, T body) {
    this.address = address;
    this.headers = headers;
    this.body = body;
  }

  public String address() {
    return address;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public T body() {
    return body;
  }
}
