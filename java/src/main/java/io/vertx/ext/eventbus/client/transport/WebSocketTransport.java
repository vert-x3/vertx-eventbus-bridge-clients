package io.vertx.ext.eventbus.client.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.vertx.ext.eventbus.client.EventBusClientOptions;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebSocketTransport extends Transport {

  private ChannelHandlerContext handlerCtx;
  private boolean handshakeComplete = false;
  private boolean reading;
  private boolean flush;

  public WebSocketTransport(EventBusClientOptions options) {
    super(options);
  }

  /**
   * Upgrades the channel to the WebSocket protocol and registers event handlers on the channel.
   * <p>
   * {@inheritDoc}
   *
   * @param channel channel to which to add the handlers to
   * @throws Exception any exception
   */
  @Override
  protected void initChannel(Channel channel) throws Exception {
    super.initChannel(channel);

    StringBuilder url = new StringBuilder();
    url.append("ws");
    if (this.options.isSsl()) {
      url.append("s");
    }
    url.append("://").append(this.options.getHost()).append(options.getWebsocketPath());

    WebSocketClientHandshaker handshaker =
      WebSocketClientHandshakerFactory.newHandshaker(new URI(url.toString()),
        WebSocketVersion.V13,
        null,
        false,
        new DefaultHttpHeaders(),
        options.getWebsocketMaxWebsocketFrameSize());
    WebSocketClientProtocolHandler handler = new WebSocketClientProtocolHandler(handshaker);

    ChannelPipeline pipeline = channel.pipeline();
    pipeline.addLast(new HttpClientCodec());
    pipeline.addLast(new HttpObjectAggregator(8192));
    pipeline.addLast(handler);
    pipeline.addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
          handlerCtx = ctx;
          handshakeComplete = true;
          connectedHandler.handle(null);
        }
      }

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        reading = true;
        if (msg instanceof BinaryWebSocketFrame) {
          BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
          String json = frame.content().toString(StandardCharsets.UTF_8);
          messageHandler.handle(json);
        } else {
          System.out.println("Unhandled " + msg);
        }
      }

      @Override
      public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        reading = false;
        if (flush) {
          flush = false;
          ctx.flush();
        }
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        handlerCtx = null;
        if (handshakeComplete) {
          closeHandler.handle(null);
        }
      }
    });
  }

  @Override
  void handshakeCompleteHandler(Channel channel) {
    // NOOP
  }

  @Override
  public void send(final String message) {
    if (handlerCtx.executor().inEventLoop()) {
      ByteBuf buff = handlerCtx.alloc().buffer();
      buff.writeCharSequence(message, StandardCharsets.UTF_8);
      BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buff);
      if (reading) {
        flush = true;
        addSendErrorHandler(handlerCtx, message, handlerCtx.write(frame));
      } else {
        addSendErrorHandler(handlerCtx, message, handlerCtx.writeAndFlush(frame));
      }
    } else {
      handlerCtx.executor().execute(new Runnable() {
        @Override
        public void run() {
          send(message);
        }
      });
    }
  }
}
