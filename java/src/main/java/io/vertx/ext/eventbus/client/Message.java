package io.vertx.ext.eventbus.client;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a message that is received from the event bus in a handler.
 * <p>
 * Messages have a {@code body}, which can be null, and also {@code headers}, which can be empty.
 * <p>
 * If the message was sent specifying a reply handler, it can be replied to using {@link #reply} or {@link #replyAndRequest}.
 * <p>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Message<T> {

  private final EventBusClient client;
  private final String address;
  private final Map<String, String> headers;
  private final T body;
  private final String replyAddress;

  /**
   * Constructor of the Message.
   *
   * @param client the {@link EventBusClient} which is used to reply the message.
   * @param address the address this Message is sent to.
   * @param headers the message headers.
   * @param body the message body, it can be null.
   * @param replyAddress the reply address, it can be null.
   */
  public Message(EventBusClient client, String address, Map<String, String> headers, T body, String replyAddress) {
    this.client = client;
    this.address = address;
    this.headers = (headers == null) ? Collections.<String, String>emptyMap() : headers;
    this.body = body;
    this.replyAddress = replyAddress;
  }

  /**
   * The address the message was sent to
   *
   * @return the address
   */
  public String address() {
    return address;
  }

  /**
   * Message headers. Can be empty
   *
   * @return the headers
   */
  public Map<String, String> headers() {
    return headers;
  }

  /**
   * The body of the message. Can be null.
   *
   * @return  the body, or null.
   */
  public T body() {
    return body;
  }

  /**
   * The reply address. Can be null.
   *
   * @return the reply address, or null, if message was sent without a reply handler.
   */
  public String replyAddress() {
    return replyAddress;
  }

  /**
   * Reply to this message.
   * <p>
   * If the message was sent specifying a reply handler, the {@code replyAddress} will be set by the bridge, and that
   * handler will be called when it has received a reply. If the message wasn't sent specifying a receipt handler
   * this method will throw {@code IllegalStateException}.
   *
   * @param body  the message to reply with.
   * @throws IllegalStateException if {@code replyAddress} is null.
   */
  public void reply(Object body) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body);
  }

  /**
   * Reply to this message with {@link DeliveryOptions} specified.
   * <p>
   * If the message was sent specifying a reply handler, the {@link #replyAddress} will be set by the bridge, and that
   * handler will be called when it has received a reply. If the message wasn't sent specifying a receipt handler
   * this method will throw {@code IllegalStateException}.
   *
   * @param body  the message to reply with.
   * @param options the {@link DeliveryOptions} used to reply the message.
   * @throws IllegalStateException if {@code replyAddress} is null.
   */
  public void reply(Object body, DeliveryOptions options) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.send(replyAddress, body, options);
  }

  /**
   * Reply to this message, specifying a {@code handler} for the reply - i.e.
   * to receive the reply to the reply.
   * <p>
   * If the message was sent specifying a reply handler, the {@code replyAddress} will be set by the bridge, and that
   * handler will be called when it has received a reply. If the message wasn't sent specifying a receipt handler
   * this method will throw {@code IllegalStateException}.
   *
   * @param body  the message to reply with.
   * @param handler he reply handler for the reply.
   * @throws IllegalStateException if {@code replyAddress} is null.
   */
  public <R> void replyAndRequest(Object body, Handler<AsyncResult<Message<R>>> handler) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.request(replyAddress, body, handler);
  }

  /**
   * Reply to this message with {@link DeliveryOptions} specified, specifying a {@code handler} for the reply - i.e.
   * to receive the reply to the reply.
   * <p>
   * If the message was sent specifying a reply handler, the {@code replyAddress} will be set by the bridge, and that
   * handler will be called when it has received a reply. If the message wasn't sent specifying a receipt handler
   * this method will throw {@code IllegalStateException}.
   *
   * @param body  the message to reply with.
   * @param options the {@link DeliveryOptions} used to reply the message.
   * @param handler he reply handler for the reply.
   * @throws IllegalStateException if {@code replyAddress} is null.
   */
  public <R> void replyAndRequest(Object body, DeliveryOptions options, Handler<AsyncResult<Message<R>>> handler) {
    if (replyAddress == null) {
      throw new IllegalStateException();
    }
    client.request(replyAddress, body, options, handler);
  }
}
