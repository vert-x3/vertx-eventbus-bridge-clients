package io.vertx.ext.eventbus.client;

/**
 * A generic event handler.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Handler<T> {

  /**
   * Something has happened, so handle it.
   *
   * @param event  the event to handle
   */
  void handle(T event);

}
