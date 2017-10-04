package io.vertx.ext.eventbus.client;

// import io.vertx.ext.eventbus.client.options.ProxyOptions;
import io.vertx.ext.eventbus.client.options.ProxyOptions;
import io.vertx.ext.eventbus.client.options.TcpTransportOptions;
// import io.vertx.ext.eventbus.client.options.TrustOptions;
import io.vertx.ext.eventbus.client.options.TrustOptions;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 *         <p>
 *         Based on io.vertx.core.net.ClientOptionsBase and others by @author Tim Fox
 */
public class EventBusClientOptions {

  /**
   * The default value for host name = "localhost"
   */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * The default value for port = 8080
   */
  public static final int DEFAULT_PORT = 8080;

  /**
   * The default value of connect timeout = 60000 ms
   */
  public static final int DEFAULT_CONNECT_TIMEOUT = 60000;

  /**
   * The default value of ping interval = 1000 ms
   */
  public static final int DEFAULT_PING_INTERVAL = 1000;

  /**
   * SSL enable by default = false
   */
  public static final boolean DEFAULT_SSL = false;

  /**
   * Default idle timeout = 0 ms (0 = disabled)
   */
  public static final int DEFAULT_IDLE_TIMEOUT = 0;

  /**
   * Default value of whether hostname verification (for SSL/TLS) is enabled = true
   */
  public static final boolean DEFAULT_VERIFY_HOST = true;

  /**
   * The default value of whether all servers (SSL/TLS) should be trusted = false
   */
  public static final boolean DEFAULT_TRUST_ALL = false;

  /**
   * The default value of whether auto reconnects are enabled, even if the client does not try to send a message = true
   */
  public static final boolean DEFAULT_AUTO_RECONNECT = true;

  /**
   * The default value of the pause between reconnect tries = 3000 ms
   */
  public static final int DEFAULT_AUTO_RECONNECT_INTERVAL = 3000;

  /**
   * The default value of the maximum number of auto reconnect tries = 0 (0 = no limit)
   */
  public static final int DEFAULT_MAX_AUTO_RECONNECT_TRIES = 0;

  private TcpTransportOptions tcpTransportOptions;
  private WebSocketTransportOptions webSocketTransportOptions;

  private String host;
  private int port;

  private boolean ssl;
  private int idleTimeout;
  private int connectTimeout;
  private int pingInterval;
  private boolean verifyHost;
  private boolean trustAll;
  private ProxyOptions proxyOptions;
  private TrustOptions trustOptions;

  private boolean autoReconnect;
  private int autoReconnectInterval;
  private int maxAutoReconnectTries;

  /**
   * Default constructor
   */
  public EventBusClientOptions() {
    this.init();
  }

  private void init() {
    this.host = DEFAULT_HOST;
    this.port = DEFAULT_PORT;

    this.ssl = DEFAULT_SSL;
    this.idleTimeout = DEFAULT_IDLE_TIMEOUT;
    this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    this.pingInterval = DEFAULT_PING_INTERVAL;
    this.verifyHost = DEFAULT_VERIFY_HOST;
    this.trustAll = DEFAULT_TRUST_ALL;
    this.proxyOptions = null;
    this.trustOptions = null;

    this.autoReconnect = DEFAULT_AUTO_RECONNECT;
    this.autoReconnectInterval = DEFAULT_AUTO_RECONNECT_INTERVAL;
    this.maxAutoReconnectTries = DEFAULT_MAX_AUTO_RECONNECT_TRIES;
  }

  /**
   * Set the TCP transport options for usage with {@link io.vertx.ext.eventbus.client.transport.TcpTransport}
   *
   * @param tcpTransportOptions TCP transport options
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setTcpTransportOptions(TcpTransportOptions tcpTransportOptions) {
    this.tcpTransportOptions = tcpTransportOptions;
    return this;
  }

  /**
   * Get the TCP transport options for usage with {@link io.vertx.ext.eventbus.client.transport.TcpTransport}
   *
   * @return TCP transport options
   */
  public TcpTransportOptions getTcpTransportOptions() {
    return this.tcpTransportOptions;
  }

  /**
   * Set the WebSocket transport options for usage with {@link io.vertx.ext.eventbus.client.transport.WebSocketTransport}
   *
   * @param webSocketTransportOptions WebSocket transport options
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setWebSocketTransportOptions(WebSocketTransportOptions webSocketTransportOptions) {
    this.webSocketTransportOptions = webSocketTransportOptions;
    return this;
  }

  /**
   * Get the WebSocket transport options for usage with {@link io.vertx.ext.eventbus.client.transport.WebSocketTransport}
   *
   * @return WebSocket transport options
   */
  public WebSocketTransportOptions getWebSocketTransportOptions() {
    return this.webSocketTransportOptions;
  }

  /**
   * Set the host to connect the client to
   *
   * @param host the host
   * @return
   */
  public EventBusClientOptions setHost(String host) {
    this.host = host;
    return this;
  }

  /**
   * @return the host to connect to
   */
  public String getHost() {
    return this.host;
  }

  /**
   * Set the port on to connect the client to
   *
   * @param port the port
   * @return
   */
  public EventBusClientOptions setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return the port to connect to
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Set whether SSL/TLS is enabled
   *
   * @param ssl true if enabled
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setSsl(boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  /**
   * @return is SSL/TLS enabled?
   */
  public boolean isSsl() {
    return this.ssl;
  }

  /**
   * Set the idle timeout, in seconds. zero means don't timeout.
   * This determines if a connection will timeout and be closed if no data is received within the timeout.
   *
   * @param idleTimeout the idle timeout, in seconds
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setIdleTimeout(int idleTimeout) {
    if (idleTimeout < 0) {
      throw new IllegalArgumentException("idleTimeout must be >= 0");
    }
    this.idleTimeout = idleTimeout;
    return this;
  }

  /**
   * @return the idle timeout, in seconds (0 means no timeout)
   */
  public int getIdleTimeout() {
    return this.idleTimeout;
  }

  /**
   * Set the connect timeout
   *
   * @param connectTimeout connect timeout, in ms
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setConnectTimeout(int connectTimeout) {
    if (connectTimeout < 0) {
      throw new IllegalArgumentException("connectTimeout must be >= 0");
    }
    this.connectTimeout = connectTimeout;
    return this;
  }

  /**
   * @return the value of connect timeout
   */
  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Set the ping interval
   *
   * @param pingInterval ping interval, in ms
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setPingInterval(int pingInterval) {
    if (pingInterval <= 0) {
      throw new IllegalArgumentException("pingInterval must be > 0");
    }
    this.pingInterval = pingInterval;
    return this;
  }

  /**
   * @return the value of ping interval
   */
  public int getPingInterval() {
    return pingInterval;
  }

  /**
   * Set whether hostname verification is enabled
   *
   * @param verifyHost true if enabled
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setVerifyHost(boolean verifyHost) {
    this.verifyHost = verifyHost;
    return this;
  }

  /**
   * Is hostname verification (for SSL/TLS) enabled?
   *
   * @return true if enabled
   */
  public boolean isVerifyHost() {
    return this.verifyHost;
  }

  /**
   * Set whether all server certificates should be trusted
   *
   * @param trustAll true if all should be trusted
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
    return this;
  }

  /**
   * @return true if all server certificates should be trusted
   */
  public boolean isTrustAll() {
    return this.trustAll;
  }

  /**
   * Set proxy options for connections via CONNECT proxy (e.g. Squid) or a SOCKS proxy.
   *
   * @param proxyOptions proxy options object
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyOptions(ProxyOptions proxyOptions) {
      this.proxyOptions = proxyOptions;
      return this;
  }

  /**
   * Get proxy options for connections
   *
   * @return proxy options
   */
  public ProxyOptions getProxyOptions() {
      return this.proxyOptions;
  }

  /**
   * Set trust options for SSL / TLS connections
   *
   * @param trustOptions trust options object
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setTrustOptions(TrustOptions trustOptions) {
      this.trustOptions = trustOptions;
      return this;
  }

  /**
   * Get trust options for SSL / TLS connections
   *
   * @return trust options
   */
  public TrustOptions getTrustOptions() {
      return this.trustOptions;
  }

  /**
   * Are auto reconnects enabled, even if the client does not try to send a message?
   *
   * @return if auto reconnects are enabled
   */
  public boolean isAutoReconnect() {
    return this.autoReconnect;
  }

  /**
   * Set whether auto reconnects are enabled, even if the client does not try to send a message
   *
   * @param autoReconnect true if auto reconnects are enabled
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setAutoReconnect(boolean autoReconnect) {
    this.autoReconnect = autoReconnect;
    return this;
  }

  /**
   * Get the length of the pause between auto reconnect tries
   *
   * @return length of the pause in ms
   */
  public int getAutoReconnectInterval() {
    return this.autoReconnectInterval;
  }

  /**
   * Set the length of the pause between auto reconnect tries
   *
   * @param autoReconnectInterval length of the pause in ms
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setAutoReconnectInterval(int autoReconnectInterval) {
    this.autoReconnectInterval = autoReconnectInterval;
    return this;
  }

  /**
   * Get the maximum number of auto reconnect tries
   *
   * @return maximum number of reconnect tries
   */
  public int getMaxAutoReconnectTries() {
    return this.maxAutoReconnectTries;
  }

  /**
   * Set maximum number of auto reconnect tries
   *
   * @param maxAutoReconnectTries maximum number of reconnect tries
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setMaxAutoReconnectTries(int maxAutoReconnectTries) {
    this.maxAutoReconnectTries = maxAutoReconnectTries;
    return this;
  }
}
