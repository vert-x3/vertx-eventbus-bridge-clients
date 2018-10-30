package io.vertx.ext.eventbus.client;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 * <p>
 * Based on io.vertx.core.net.ClientOptionsBase and others by @author Tim Fox
 */
public class EventBusClientOptions {

  /**
   * The default value for host name = "localhost"
   */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * The default value for port = -1 which means 7000 for TCP and 80 for HTTP client
   */
  public static final int DEFAULT_PORT = -1;

  /**
   * The default value of connect timeout = 60000 ms
   */
  public static final int DEFAULT_CONNECT_TIMEOUT = 60000;

  /**
   * The default value of ping interval = 5000 ms
   */
  public static final int DEFAULT_PING_INTERVAL = 5000;

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

  /**
   * The default storePath to connect the WebSocket client to = /eventbus/websocket
   */
  public static final String DEFAULT_WEBSOCKET_PATH = "/eventbus/websocket";

  /**
   * The default value for maximum websocket frame size = 65536 bytes
   */
  public static final int DEFAULT_MAX_WEBSOCKET_FRAME_SIZE = 65536;

  private String host;
  private int port;

  private boolean ssl;
  private String trustStorePath;
  private String trustStorePassword;
  private String trustStoreType = "jks";
  private boolean verifyHost;
  private boolean trustAll;

  private int idleTimeout;
  private int pingInterval;

  private int connectTimeout;
  private boolean autoReconnect;
  private int autoReconnectInterval;
  private int maxAutoReconnectTries;

  private String proxyHost;
  private int proxyPort;
  private String proxyUsername;
  private String proxyPassword;
  private ProxyType proxyType;

  private String websocketPath;
  private int websocketMaxWebsocketFrameSize;

  /**
   * Default constructor
   */
  public EventBusClientOptions() {
    this.host = DEFAULT_HOST;
    this.port = DEFAULT_PORT;
    this.ssl = DEFAULT_SSL;
    this.idleTimeout = DEFAULT_IDLE_TIMEOUT;
    this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    this.pingInterval = DEFAULT_PING_INTERVAL;
    this.verifyHost = DEFAULT_VERIFY_HOST;
    this.trustAll = DEFAULT_TRUST_ALL;
    this.autoReconnect = DEFAULT_AUTO_RECONNECT;
    this.autoReconnectInterval = DEFAULT_AUTO_RECONNECT_INTERVAL;
    this.maxAutoReconnectTries = DEFAULT_MAX_AUTO_RECONNECT_TRIES;
    this.websocketPath = EventBusClientOptions.DEFAULT_WEBSOCKET_PATH;
    this.websocketMaxWebsocketFrameSize = EventBusClientOptions.DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;
  }

  /**
   * Copy constructor
   */
  public EventBusClientOptions(EventBusClientOptions options) {
    this.host = options.host;
    this.port = options.port;
    this.ssl = options.ssl;
    this.trustStorePath = options.trustStorePath;
    this.trustStorePassword = options.trustStorePassword;
    this.trustStoreType = options.trustStoreType;
    this.verifyHost = options.verifyHost;
    this.trustAll = options.trustAll;
    this.idleTimeout = options.idleTimeout;
    this.pingInterval = options.pingInterval;
    this.connectTimeout = options.connectTimeout;
    this.autoReconnect = options.autoReconnect;
    this.autoReconnectInterval = options.autoReconnectInterval;
    this.maxAutoReconnectTries = options.maxAutoReconnectTries;
    this.proxyHost = options.proxyHost;
    this.proxyPort = options.proxyPort;
    this.proxyType = options.proxyType;
    this.proxyPassword = options.proxyPassword;
    this.proxyUsername = options.proxyUsername;
    this.websocketPath = options.websocketPath;
    this.websocketMaxWebsocketFrameSize = options.websocketMaxWebsocketFrameSize;
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
   * @param idleTimeout the idle timeout, in milliseconds
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
   * @return the idle timeout, in milliseconds (0 means no timeout)
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
   * Set whether all server certificates should be trusted (if so, hosts are never verified)
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

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public EventBusClientOptions setTrustStorePath(String trustStorePath) {
    this.trustStorePath = trustStorePath;
    return this;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public EventBusClientOptions setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
    return this;
  }

  public String getTrustStoreType() {
    return trustStoreType;
  }

  public EventBusClientOptions setTrustStoreType(String trustStoreType) {
    if (!Arrays.asList("jks", "pfx", "pem", null).contains(trustStoreType)) {
      throw new IllegalArgumentException("Invalid trust store type it must be one of jks (Java), pfx (PKCS12) or pem (PKCS8)");
    }
    this.trustStoreType = trustStoreType;
    return this;
  }

  /**
   * Set the path to connect the WebSocket client to
   *
   * @param websocketPath the storePath
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setWebsocketPath(String websocketPath) {
    this.websocketPath = websocketPath;
    return this;
  }

  /**
   * Get the path to connect the WebSocket client to
   *
   * @return the storePath
   */
  public String getWebsocketPath() {
    return this.websocketPath;
  }

  /**
   * Get the maximum websocket framesize to use
   *
   * @return the max websocket framesize
   */
  public int getWebsocketMaxWebsocketFrameSize() {
    return this.websocketMaxWebsocketFrameSize;
  }

  /**
   * Set the max websocket frame size
   *
   * @param websocketMaxWebsocketFrameSize the max frame size, in bytes
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setWebsocketMaxWebsocketFrameSize(int websocketMaxWebsocketFrameSize) {
    this.websocketMaxWebsocketFrameSize = websocketMaxWebsocketFrameSize;
    return this;
  }

  /**
   * Get proxy host.
   *
   * @return proxy hosts
   */
  public String getProxyHost() {
    return this.proxyHost;
  }

  /**
   * Set proxy host.
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyHost(String proxyHost) {
    Objects.requireNonNull(proxyHost, "Proxy host may not be null");
    this.proxyHost = proxyHost;
    return this;
  }

  /**
   * Get proxy port.
   *
   * @return proxy port
   */
  public int getProxyPort() {
    return this.proxyPort;
  }

  /**
   * Set proxy port.
   *
   * @param proxyPort the proxy port to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyPort(int proxyPort) {
    if (proxyPort < 0 || proxyPort > 65535) {
      throw new IllegalArgumentException("Invalid proxy port " + proxyPort);
    }
    this.proxyPort = proxyPort;
    return this;
  }

  /**
   * Get proxy username.
   *
   * @return proxy proxyUsername
   */
  public String getProxyUsername() {
    return this.proxyUsername;
  }

  /**
   * Set proxy username.
   *
   * @param username the proxy proxyUsername
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyUsername(String username) {
    this.proxyUsername = username;
    return this;
  }

  /**
   * Get proxy storePassword.
   *
   * @return proxy storePassword
   */
  public String getProxyPassword() {
    return this.proxyPassword;
  }

  /**
   * Set proxy password.
   *
   * @param password the proxy storePassword
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyPassword(String password) {
    this.proxyPassword = password;
    return this;
  }

  /**
   * Get proxy type.
   *
   * <p>type can be HTTP, SOCKS4 and SOCKS5
   *
   * @return proxy type
   */
  public ProxyType getProxyType() {
    return this.proxyType;
  }

  /**
   * Set proxy type.
   *
   * <p>ProxyType can be HTTP, SOCKS4 and SOCKS5
   *
   * @param proxyType the proxy type to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClientOptions setProxyType(ProxyType proxyType) {
    Objects.requireNonNull(proxyType, "Proxy proxyType may not be null");
    this.proxyType = proxyType;
    return this;
  }
}
