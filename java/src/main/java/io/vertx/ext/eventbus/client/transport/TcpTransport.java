package io.vertx.ext.eventbus.client.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.vertx.ext.eventbus.client.EventBusClientOptions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TcpTransport extends Transport {

  private ChannelHandlerContext handlerCtx;
  private boolean baseHandshakeComplete = false;
  private boolean tcpHandshakeComplete = false;
  private AtomicBoolean connectedHandlerInvoked = new AtomicBoolean(false);
  private boolean reading;
  private boolean flush;

  public TcpTransport(EventBusClientOptions options) {
    super(options);
  }

  /**
   * Registers event handlers on the channel.
   * <p>
   * {@inheritDoc}
   *
   * @param channel channel to which to add the handlers to
   * @throws Exception any exception
   */
  @Override
  protected void initChannel(Channel channel) throws Exception {
    super.initChannel(channel);

    baseHandshakeComplete = false;
    tcpHandshakeComplete = false;
    connectedHandlerInvoked.set(false);

    channel.pipeline().addLast(new ByteToMessageDecoder() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        reading = true;
        super.channelRead(ctx, msg);
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
      public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        handlerCtx = ctx;
        tcpHandshakeComplete = true;
        if (baseHandshakeComplete && !connectedHandlerInvoked.getAndSet(true)) {
          connectedHandler.handle(null);
        }
      }

      @Override
      protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
          if (in.readableBytes() < 4) {
            break;
          }
          int readerIdx = in.readerIndex();
          int len = in.getInt(readerIdx);
          if (in.readableBytes() < 4 + len) {
            return;
          }
          String json = in.toString(readerIdx + 4, len, StandardCharsets.UTF_8);
          in.readerIndex(readerIdx + 4 + len);
          messageHandler.handle(json);
        }
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        handlerCtx = null;
        if (connectedHandlerInvoked.get()) {
          closeHandler.handle(null);
        }
      }
    });
  }

  @Override
  void handshakeCompleteHandler(Channel channel) {
    baseHandshakeComplete = true;
    if (tcpHandshakeComplete && !connectedHandlerInvoked.getAndSet(true)) {
      connectedHandler.handle(null);
    }
  }

  @Override
  public void send(final String message) {
    if (handlerCtx.executor().inEventLoop()) {
      ByteBuf buff = handlerCtx.alloc().buffer();
      buff.writeInt(0);
      buff.writeCharSequence(message, StandardCharsets.UTF_8);
      buff.setInt(0, buff.readableBytes() - 4);
      if (reading) {
        flush = true;
        addSendErrorHandler(handlerCtx, message, handlerCtx.write(buff));
      } else {
        addSendErrorHandler(handlerCtx, message, handlerCtx.writeAndFlush(buff));
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
