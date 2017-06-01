package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncResult<T> {

  private final T result;
  private final Throwable cause;

  public AsyncResult(T result, Throwable cause) {
    this.result = result;
    this.cause = cause;
  }

  public boolean failed() {
    return cause != null;
  }

  public boolean succeeded() {
    return cause == null;
  }

  public T result() {
    return result;
  }

  public Throwable cause() {
    return cause;
  }
}
