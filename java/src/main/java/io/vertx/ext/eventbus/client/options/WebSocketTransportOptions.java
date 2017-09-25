package io.vertx.ext.eventbus.client.options;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 *         <p>
 *         Based on io.vertx.core.http.HttpClientOptions @author Tim Fox
 */
public class WebSocketTransportOptions {

  /**
   * The default path to connect the WebSocket client to = /eventbus/websocket
   */
  public static final String DEFAULT_PATH = "/eventbus/websocket";

  /**
   * The default value for maximum websocket frame size = 65536 bytes
   */
  public static final int DEFAULT_MAX_WEBSOCKET_FRAME_SIZE = 65536;

  private String path;
  private int maxWebsocketFrameSize;

  public WebSocketTransportOptions() {
    init();
  }

  private void init() {
    this.path = DEFAULT_PATH;
    this.maxWebsocketFrameSize = DEFAULT_MAX_WEBSOCKET_FRAME_SIZE;
  }

  /**
   * Set the path to connect the WebSocket client to
   *
   * @param path the path
   * @return a reference to this, so the API can be used fluently
   */
  public WebSocketTransportOptions setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * Get the path to connect the WebSocket client to
   *
   * @return the path
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Get the maximum websocket framesize to use
   *
   * @return the max websocket framesize
   */
  public int getMaxWebsocketFrameSize() {
    return this.maxWebsocketFrameSize;
  }

  /**
   * Set the max websocket frame size
   *
   * @param maxWebsocketFrameSize the max frame size, in bytes
   * @return a reference to this, so the API can be used fluently
   */
  public WebSocketTransportOptions setMaxWebsocketFrameSize(int maxWebsocketFrameSize) {
    this.maxWebsocketFrameSize = maxWebsocketFrameSize;
    return this;
  }
}
