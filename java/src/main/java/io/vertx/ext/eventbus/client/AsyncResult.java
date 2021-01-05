package io.vertx.ext.eventbus.client;

/**
 * Encapsulates the result of an asynchronous operation.
 * <p>
 * The result can either have failed or succeeded.
 * <p>
 * If it failed then the cause of the failure is available with {@link #cause}.
 * <p>
 * If it succeeded then the actual result is available with {@link #result}
 *
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

  /**
   * Did it fail?
   *
   * @return true if it failed or false otherwise
   */
  public boolean failed() {
    return cause != null;
  }

  /**
   * Did it succeed?
   *
   * @return true if it succeeded or false otherwise
   */
  public boolean succeeded() {
    return cause == null;
  }

  /**
   * The result of the operation. This will be null if the operation failed.
   *
   * @return the result or null if the operation failed.
   */
  public T result() {
    return result;
  }

  /**
   * A Throwable describing failure. This will be null if the operation succeeded.
   *
   * @return the cause or null if the operation succeeded.
   */
  public Throwable cause() {
    return cause;
  }
}
