package io.vertx.ext.eventbus.client;

/**
 * A {@link RuntimeException} represents the exception happened in client.
 * <p>
 * Normally it is thrown when preparing the client setup, especially on building the {@link javax.net.ssl.SSLContext}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientException extends RuntimeException {

  /**
   * Constructor of ClientException.
   *
   * @param cause the cause of the Exception.
   */
  public ClientException(Throwable cause) {
    super(cause);
  }
}
