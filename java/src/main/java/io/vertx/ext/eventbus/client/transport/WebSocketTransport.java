package io.vertx.ext.eventbus.client.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.Future;
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

  @Override
  protected void initChannel(Channel channel) throws Exception {
    super.initChannel(channel);

    StringBuilder url = new StringBuilder();
    url.append("ws");
    if(this.options.isSsl()) {
      url.append("s");
    }
    url.append("://").append(this.options.getHost()).append(this.options.getWebSocketTransportOptions().getPath());

    WebSocketClientHandshaker handshaker =
      WebSocketClientHandshakerFactory.newHandshaker(new URI(url.toString()),
                                                     WebSocketVersion.V13,
                                                     null,
                                                     false,
                                                     new DefaultHttpHeaders(),
                                                     this.options.getWebSocketTransportOptions().getMaxWebsocketFrameSize());
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
        if(handshakeComplete) {
          closeHandler.handle(null);
        }
      }
    });
  }

  @Override
  public void sslHandshakeHandler(Future<Channel> future) {
    if(!future.isSuccess()) {
      handleError("An exception occured during TLS handshake with server.", future.cause());
    }
  }

  @Override
  public void send(final String message) {
    if (handlerCtx.executor().inEventLoop()) {
      ByteBuf buff = handlerCtx.alloc().buffer();
      buff.writeCharSequence(message, StandardCharsets.UTF_8);
      BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buff);
      if (reading) {
        flush = true;
        handlerCtx.write(frame);
      } else {
        handlerCtx.writeAndFlush(frame);
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
