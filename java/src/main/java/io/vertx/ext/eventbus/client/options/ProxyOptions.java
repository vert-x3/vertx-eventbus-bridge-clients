package io.vertx.ext.eventbus.client.options;

import java.util.Objects;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 *
 * Blatantly copied from io.vertx.core.net.ProxyOptions @author Alexander Lehmann
 */
public abstract class ProxyOptions {

  /**
   * The default hostname for proxy connect = "localhost"
   */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * The default port for proxy connect = 3128
   *
   * 3128 is the default port for e.g. Squid
   */
  public static final int DEFAULT_PORT = 3128;

  /**
   * The default proxy type (HTTP)
   */
  public static final ProxyType DEFAULT_TYPE = ProxyType.HTTP;

  private String host; // Todo
  private int port; // Todo
  private String username; // Todo
  private String password; // Todo
  private ProxyType type; // Todo

  /**
   * Default constructor.
   */
  public ProxyOptions() {
    host = DEFAULT_HOST;
    port = DEFAULT_PORT;
    type = DEFAULT_TYPE;
  }

  /**
   * Get proxy host.
   *
   * @return  proxy hosts
   */
  public String getHost() {
    return host;
  }

  /**
   * Set proxy host.
   *
   * @param host the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setHost(String host) {
    Objects.requireNonNull(host, "Proxy host may not be null");
    this.host = host;
    return this;
  }

  /**
   * Get proxy port.
   *
   * @return  proxy port
   */
  public int getPort() {
    return port;
  }

  /**
   * Set proxy port.
   *
   * @param port the proxy port to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setPort(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Invalid proxy port " + port);
    }
    this.port = port;
    return this;
  }

  /**
   * Get proxy username.
   *
   * @return  proxy username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set proxy username.
   *
   * @param username the proxy username
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * Get proxy password.
   *
   * @return  proxy password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set proxy password.
   *
   * @param password the proxy password
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get proxy type.
   *
   *<p>ProxyType can be HTTP, SOCKS4 and SOCKS5
   *
   * @return  proxy type
   */
  public ProxyType getType() {
    return type;
  }

  /**
   * Set proxy type.
   *
   * <p>ProxyType can be HTTP, SOCKS4 and SOCKS5
   *
   * @param type the proxy type to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setType(ProxyType type) {
    Objects.requireNonNull(type, "Proxy type may not be null");
    this.type = type;
    return this;
  }
}
