package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
interface MessageHandler<T> {

  String address();

  void handleMessage(Message<T> msg);

  void handleError(Throwable err);

}
