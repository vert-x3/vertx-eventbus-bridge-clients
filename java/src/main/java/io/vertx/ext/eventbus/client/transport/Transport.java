package io.vertx.ext.eventbus.client.transport;

import io.netty.channel.*;
import io.netty.handler.proxy.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.vertx.ext.eventbus.client.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class Transport extends ChannelInitializer {

  final EventBusClientOptions options;
  private final InternalLogger logger;
  private final SslContext sslContext;

  Handler<Void> connectedHandler;
  Handler<String> messageHandler;
  Handler<Void> closeHandler;

  private Handler<Throwable> exceptionHandler;

  Transport(EventBusClientOptions options) {

    SslContext sslContext;
    try {
      sslContext = build(options);
    } catch (Exception e) {
      throw new ClientException(e);
    }

    this.options = options;
    this.logger = InternalLoggerFactory.getInstance(Transport.class);
    this.sslContext = sslContext;
  }

  private static SslContext build(EventBusClientOptions options) throws Exception {
    SslContextBuilder builder = SslContextBuilder.forClient();
    if (options.isTrustAll()) {
      builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
    } else if (options.getTrustStorePath() != null) {

      String storePath = options.getTrustStorePath();
      InputStream storeIS = null;
      if (storePath != null) {
        File f = new File(storePath);
        if (f.exists() && f.isFile()) {
          // Ok
          storeIS = new FileInputStream(f);
        } else {
          storeIS= Thread.currentThread().getContextClassLoader().getResourceAsStream(storePath);
        }
      }
      if (storeIS == null) {
        throw new IllegalArgumentException("Store file not found:" + storePath);
      }
      if ("pem".equals(options.getTrustStoreType())) {
        builder.trustManager(storeIS);
      } else {
        KeyStore keyStore;
        if ("jks".equals(options.getTrustStoreType())) {
          keyStore = KeyStore.getInstance("jks");
        } else {
          keyStore = KeyStore.getInstance("pkcs12");
        }
        if (options.getTrustStorePassword() != null) {
          keyStore.load(storeIS, options.getTrustStorePassword().toCharArray());
        } else {
          keyStore.load(storeIS, null);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        builder.trustManager(trustManagerFactory);
      }
    }
    return builder.build();
  }

  /**
   * Adds Proxy, TLS and idle timeout handlers to the channel pipeline.
   *
   * @param channel channel to which to add the handlers to
   * @throws Exception any exception
   */
  @Override
  protected void initChannel(final Channel channel) throws Exception {
    final ChannelPipeline pipeline = channel.pipeline();

    channel.config().setConnectTimeoutMillis(this.options.getConnectTimeout());

    if (this.options.getProxyHost() == null && !this.options.isSsl()) {
      pipeline.addLast(new ChannelInboundHandlerAdapter() {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
          super.channelActive(ctx);
          Transport.this.handshakeCompleteHandler(channel);
        }
      });
    }

    if (this.options.getProxyHost() != null) {

      String proxyHost = options.getProxyHost();
      int proxyPort = options.getProxyPort();
      String proxyUsername = options.getProxyUsername();
      String proxyPassword = options.getProxyPassword();
      ProxyType proxyType = options.getProxyType();
      SocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);

      final ProxyHandler proxyHandler;

      switch (proxyType) {
        default:
        case HTTP:
          proxyHandler = proxyUsername != null && proxyPassword != null ?
            new HttpProxyHandler(proxyAddress, proxyUsername, proxyPassword) : new HttpProxyHandler(proxyAddress);
          break;
        case SOCKS4:
          proxyHandler = proxyUsername != null ?
            new Socks4ProxyHandler(proxyAddress, proxyUsername) : new Socks4ProxyHandler(proxyAddress);
          break;
        case SOCKS5:
          proxyHandler = proxyUsername != null && proxyPassword != null ?
            new Socks5ProxyHandler(proxyAddress, proxyUsername, proxyPassword) : new Socks5ProxyHandler(proxyAddress);
          break;
      }

      pipeline.addLast("proxyHandler", proxyHandler);
      pipeline.addLast(new ChannelInboundHandlerAdapter() {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
          if (evt instanceof ProxyConnectionEvent) {
            pipeline.remove(proxyHandler);
            pipeline.remove(this);
            if (!Transport.this.options.isSsl()) {
              Transport.this.handshakeCompleteHandler(channel);
            }
          }
          ctx.fireUserEventTriggered(evt);
        }
      });
      pipeline.addLast("proxyExceptionHandler", new ChannelHandlerAdapter() {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          handleError("A proxy exception occured.", cause);
        }
      });
    }

    if (this.options.isSsl()) {
      SSLParameters sslParams = new SSLParameters();
      if (options.isVerifyHost()) {
        sslParams.setEndpointIdentificationAlgorithm("HTTPS");
      }
      SSLEngine sslEngine = sslContext.newEngine(channel.alloc(), this.options.getHost(), this.options.getPort());
      sslEngine.setUseClientMode(true);
      sslEngine.setSSLParameters(sslParams);
      SslHandler sslHandler = new SslHandler(sslEngine, false);
      sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
        @Override
        public void operationComplete(Future<Channel> future) {
          if (future.isSuccess()) {
            Transport.this.handshakeCompleteHandler(future.getNow());
          }
        }
      });
      pipeline.addLast("sslHandler", sslHandler);
      pipeline.addLast("sslExceptionHandler", new ChannelHandlerAdapter() {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          handleError("A TLS exception occured.", cause);
        }
      });
    }

    if (this.options.getIdleTimeout() > 0) {
      pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, this.options.getIdleTimeout(), TimeUnit.MILLISECONDS));
      pipeline.addLast("idleEventHandler", new ChannelDuplexHandler() {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
          if (evt instanceof IdleStateEvent) {
            ctx.close();
          }
        }
      });
    }
  }

  public void connectedHandler(Handler<Void> handler) {
    connectedHandler = handler;
  }

  public void messageHandler(Handler<String> handler) {
    messageHandler = handler;
  }

  public void closeHandler(Handler<Void> handler) {
    closeHandler = handler;
  }

  public void setExceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
  }

  protected void handleError(String message, Throwable t) {
    this.logger.error(message, t);
    Handler<Throwable> handler = this.exceptionHandler;
    if (handler != null) {
      try {
        handler.handle(t);
      } catch (Exception e) {
        // Exception handler should not throw
        e.printStackTrace();
      }
    }
  }

  /**
   * Transports can use this method to implement error handling for messages sent to the server.
   *
   * @param handlerCtx the channel context
   * @param message    the message being sent
   * @param future     the channel future created by a {@code write} method
   */
  void addSendErrorHandler(final ChannelHandlerContext handlerCtx, final String message, ChannelFuture future) {
    future.addListener(new GenericFutureListener<Future<Void>>() {
      @Override
      public void operationComplete(Future<Void> future) {
        // Suppress "Could not send because connection is closed" and SSLExceptions, as they are handled in sslExceptionHandler
        //noinspection ThrowableResultOfMethodCallIgnored
        if (!future.isSuccess() && handlerCtx.channel().isOpen() && !(future.cause() instanceof SSLException)) {
          if (message.length() > EventBusClient.MESSAGE_PRINT_LIMIT) {
            handleError("Could not send message with " + message.length() + " chars.", future.cause());
          } else {
            handleError("Could not send message: " + message, future.cause());
          }
        }
      }
    });
  }

  /**
   * This method is being called by {@code Transport} when the proxy & TLS handshake has been completed successfully.
   * <p>
   * It needs to be overriden by classes inheriting {@code Transport} and can be used to call the connectedHandler,
   * if there are no more initializing tasks to be done (e.g. WebSocket handshake).
   *
   * @param channel the channel for which the TLS handshake has been completed
   */
  abstract void handshakeCompleteHandler(Channel channel);

  /**
   * This method needs to be overriden by {@code Transport} implementations.
   * It is being invoked by {@code EventBusClient} when a message needs to be sent.
   * Transports can pass {@code ChannelFuture}s created by {@code write} methods to {@code addSendErrorHandler} to
   * implement error handling for failed messages.
   *
   * @param message the message to be send
   */
  public abstract void send(String message);
}
