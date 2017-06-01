package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Message<T> {

  private final String address;
  private final T body;

  public Message(String address, T body) {
    this.address = address;
    this.body = body;
  }

  public String address() {
    return address;
  }

  public T body() {
    return body;
  }
}
