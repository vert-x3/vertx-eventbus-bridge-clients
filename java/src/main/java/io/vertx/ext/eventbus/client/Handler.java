package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Handler<T> {

  void handle(T event);

}
