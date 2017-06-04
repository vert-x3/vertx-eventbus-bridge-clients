package io.vertx.ext.eventbus.client.transport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TcpTransport extends Transport {

  private ChannelHandlerContext handlerCtx;
  private boolean reading;
  private boolean flush;

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ch.pipeline().addLast(new ByteToMessageDecoder() {
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
        connectedHandler.handle(null);
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
        closeHandler.handle(null);
      }
    });
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
        handlerCtx.write(buff);
      } else {
        handlerCtx.writeAndFlush(buff);
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
