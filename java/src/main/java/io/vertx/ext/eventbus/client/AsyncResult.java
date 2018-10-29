package io.vertx.ext.eventbus.client;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncResult<T> {

  private final T result;
  private final Throwable cause;

  private AsyncResult(T result, Throwable cause) {
    this.result = result;
    this.cause = cause;
  }

  static <Y> AsyncResult<Y> success(Y result) {
    return new AsyncResult<Y>(result, null);
  }

  static <Y> AsyncResult<Y> failure(Throwable cause) {
    return new AsyncResult<Y>(null, cause);
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
