package io.vertx.ext.eventbus.client;

import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Message<T> {

  private final EventBusClient client;
  private final String address;
  private final Map<String, String> headers;
  private final T body;
  private final String replyAddress;

  public Message(EventBusClient client, String address, Map<String, String> headers, T body, String replyAddress) {
    this.client = client;
    this.address = address;
    this.headers = headers;
    this.body = body;
    this.replyAddress = replyAddress;
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

  public String replyAddress() {
    return replyAddress;
  }

  public void reply(Object body) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body);
  }

  public void reply(Object body, DeliveryOptions options) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body, options);
  }

  public <R> void reply(Object body, Handler<AsyncResult<Message<R>>> handler) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body, handler);
  }

  public <R> void reply(Object body, DeliveryOptions options, Handler<AsyncResult<Message<R>>> handler) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body, options, handler);
  }
}
