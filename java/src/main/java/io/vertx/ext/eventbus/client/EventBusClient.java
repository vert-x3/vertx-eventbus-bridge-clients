package io.vertx.ext.eventbus.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.ext.eventbus.client.json.GsonCodec;
import io.vertx.ext.eventbus.client.json.JsonCodec;
import io.vertx.ext.eventbus.client.logging.Logger;
import io.vertx.ext.eventbus.client.logging.LoggerFactory;
import io.vertx.ext.eventbus.client.options.TcpTransportOptions;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;
import io.vertx.ext.eventbus.client.transport.TcpTransport;
import io.vertx.ext.eventbus.client.transport.Transport;
import io.vertx.ext.eventbus.client.transport.WebSocketTransport;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventBusClient {

  public static EventBusClient tcp(EventBusClientOptions eventBusClientOptions) {
    return EventBusClient.tcp(eventBusClientOptions, new GsonCodec());
  }

  public static EventBusClient tcp(EventBusClientOptions eventBusClientOptions, JsonCodec jsonCodec) {

    if (eventBusClientOptions == null) {
      eventBusClientOptions = new EventBusClientOptions();
    }
    if(eventBusClientOptions.getTcpTransportOptions() == null) {
      eventBusClientOptions.setTcpTransportOptions(new TcpTransportOptions());
    }

    return new EventBusClient(new TcpTransport(eventBusClientOptions), eventBusClientOptions, jsonCodec);
  }

  public static EventBusClient websocket(EventBusClientOptions eventBusClientOptions) {
    return EventBusClient.websocket(eventBusClientOptions, new GsonCodec());
  }

  public static EventBusClient websocket(EventBusClientOptions eventBusClientOptions, JsonCodec jsonCodec) {

    if (eventBusClientOptions == null) {
      eventBusClientOptions = new EventBusClientOptions();
    }
    if(eventBusClientOptions.getWebSocketTransportOptions() == null) {
      eventBusClientOptions.setWebSocketTransportOptions(new WebSocketTransportOptions());
    }

    return new EventBusClient(new WebSocketTransport(eventBusClientOptions), eventBusClientOptions, jsonCodec);
  }

  private DeliveryOptions defaultOptions = new DeliveryOptions();
  private final Transport transport;
  private final NioEventLoopGroup group = new NioEventLoopGroup(1);
  private Bootstrap bootstrap;
  private final EventBusClientOptions eventBusClientOptions;
  private final JsonCodec codec;
  private Logger logger;

  private final ConcurrentMap<String, HandlerList> consumerMap = new ConcurrentHashMap<>();
  private ScheduledFuture<?> pingPeriodic;
  private ChannelFuture connectFuture;
  private Channel channel;
  private ScheduledFuture<?> reconnectFuture;
  private boolean initializedTransport;
  private boolean connected;
  private boolean closed = false;
  private int reconnectTries;

  private Handler<Handler<Void>> connectedHandler;
  private volatile Handler<Throwable> exceptionHandler;
  private Handler<Void> closeHandler;

  public EventBusClient(Transport transport, EventBusClientOptions eventBusClientOptions, JsonCodec jsonCodec) {
    this.transport = transport;
    this.bootstrap = new Bootstrap().group(this.group);
    this.eventBusClientOptions = eventBusClientOptions;
    this.codec = jsonCodec;
    this.logger = LoggerFactory.getLogger(EventBusClient.class);
  }

  private ArrayDeque<Handler<Transport>> pendingTasks = new ArrayDeque<>();

  private synchronized void execute(Handler<Transport> task) {
    if (connected) {
      task.handle(transport);
    } else if(closed) {
      logger.error("This EventBusClient is closed.");
    } else {
      pendingTasks.add(task);
      if (connectFuture == null && reconnectFuture == null) {
        initializeTransport();
        logger.info("Connecting for executing task...");
        connectTransport();
      }
    }
  }

  private synchronized void initializeTransport() {

    if(initializedTransport) {
      return;
    }
    initializedTransport = true;

    transport.connectedHandler(new Handler<Void>() {
      @Override
      public void handle(Void v) {
        synchronized (EventBusClient.this) {

          logger.info("Connected to bridge.");
          // TODO: send only when client was idle for pingInterval?
          pingPeriodic = group.next().scheduleAtFixedRate(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                              Map<String, String> msg = Collections.singletonMap("type", "ping");
                                                              send(codec.encode(msg));
                                                            }
                                                          },
            EventBusClient.this.eventBusClientOptions.getPingInterval(),
            EventBusClient.this.eventBusClientOptions.getPingInterval(),
            TimeUnit.MILLISECONDS);
          connected = true;
          // TODO: only reset this to 0 when this was no short lived connection (e.g. < 5 secs)?
          reconnectTries = 0;
          channel = connectFuture.channel();
          connectFuture = null;

          if(EventBusClient.this.connectedHandler != null)  {

            EventBusClient.this.connectedHandler.handle(new Handler<Void>() {
              @Override
              public void handle(Void v) {
                EventBusClient.this.handlePendingTasks();
              }
            });
          }
          else  {
            EventBusClient.this.handlePendingTasks();
          }
        }
      }
    });
    transport.messageHandler(new Handler<String>() {
      @Override
      public void handle(String json) {
        Map msg = codec.decode(json, Map.class);
        handleMsg(msg);
      }
    });
    transport.closeHandler(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        synchronized (EventBusClient.this) {
          logger.info("Closed connection to bridge.");
          connected = false;
          channel = null;
          if (closeHandler != null) {
            closeHandler.handle(null);
          }
          pingPeriodic.cancel(false);
          autoReconnect();
        }
      }
    });

    bootstrap.channel(NioSocketChannel.class);
    bootstrap.handler(transport);
  }

  private synchronized void connectTransport() {

    if(connected || closed || connectFuture != null || reconnectFuture != null) {
      return;
    }

    String host = EventBusClient.this.eventBusClientOptions.getHost();
    Integer port = EventBusClient.this.eventBusClientOptions.getPort();

    if(EventBusClient.this.eventBusClientOptions.getProxyOptions() != null) {
      logger.info("Connecting to bridge at " + host + ":" + port + " (via " + EventBusClient.this.eventBusClientOptions.getProxyOptions().toString() + ")...");
    } else {
      logger.info("Connecting to bridge at " + host + ":" + port + "...");
    }

    connectFuture = bootstrap.connect(host, port)
      .addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future future) {

        if(!future.isSuccess()) {
          handleError("Connecting to bridge failed.", future.cause());
          connectFuture = null;
          autoReconnect();
        }
      }
    });
  }

  private synchronized void autoReconnect() {

    if(!closed &&
       reconnectFuture == null &&
       EventBusClient.this.eventBusClientOptions.isAutoReconnect() &&
      (EventBusClient.this.eventBusClientOptions.getMaxAutoReconnectTries() == 0 ||
        reconnectTries < EventBusClient.this.eventBusClientOptions.getMaxAutoReconnectTries()))
    {
      ++reconnectTries;
      int interval = EventBusClient.this.eventBusClientOptions.getAutoReconnectInterval();
      logger.info("Auto reconnecting in " + interval + "ms (try number " + reconnectTries + ")...");
      reconnectFuture = group.next().schedule(new Runnable() {
        @Override
        public void run() {
          logger.info("Auto reconnecting...", reconnectFuture);
          reconnectFuture = null;
          connectTransport();
        }
      }, interval, TimeUnit.MILLISECONDS);
    }
  }

  private void handlePendingTasks() {

    // First register, then send pending tasks, as those tasks may result in messages being sent to registered channels
    consumerMap.keySet().forEach(this::sendRegistration);

    Handler<Transport> t;
    while ((t = this.pendingTasks.poll()) != null) {
      t.handle(this.transport);
    }
  }

  private void handleMsg(Map msg) {
    String type = (String) msg.get("type");
    if (type != null) {
      switch (type) {
        case "message":
        case "rec": {
          String address = (String) msg.get("address");
          if (address == null) {
            // TCP bridge that replies an error...
            return;
          }
          logger.info("Received message for address: " + address);
          HandlerList consumers = consumerMap.get(address);
          if (consumers != null) {
            Map body = (Map) msg.get("body");
            Map<String, String> headers;
            Map msgHeaders = (Map) msg.get("headers");
            if (msgHeaders == null) {
              headers = Collections.emptyMap();
            } else {
              headers = (Map<String, String>) msgHeaders;
            }
            String replyAddress = (String) msg.get("replyAddress");
            consumers.send(new Message(this, address, headers, body, replyAddress));
          }
          break;
        }
        case "err": {
          String address = (String) msg.get("address");
          String message = (String) msg.get("message");
//          int failureCode = msg.get("failureCode").getAsInt();
//          String failureType = msg.get("failureType").getAsString();
          HandlerList consumers = consumerMap.get(address);
          if (consumers != null) {
            consumers.fail(new RuntimeException(message));
          }
        }
      }
    }
  }

  public EventBusClient setDefaultDeliveryOptions(DeliveryOptions defaultOptions) {
    this.defaultOptions = defaultOptions;
    return this;
  }

  public EventBusClient connect() {
    closed = false;
    initializeTransport();
    logger.info("Connecting as requested...");
    connectTransport();
    return this;
  }

  public boolean isConnected() {
    return connected;
  }

  public void close() {
    if(channel != null) {
      channel.close();
    }
    closed = true;
  }

  /**
   * Sends a message.
   * <p>
   * The message will be delivered to at most one of the handlers registered to the address.
   *
   * @param address  the address to send it to
   * @param message  the message, may be {@code null}
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClient send(String address, Object message) {
    send(address, message, defaultOptions, null);
    return this;
  }

  /**
   * Sends a message.
   * <p>
   * The message will be delivered to at most one of the handlers registered to the address.
   *
   * @param address  the address to send it to
   * @param message  the message, may be {@code null}
   * @return a reference to this, so the API can be used fluently
   */
  public <T> EventBusClient send(String address, Object message, final Handler<AsyncResult<Message<T>>> replyHandler) {
    return send(address, message, defaultOptions, replyHandler);
  }

  /**
   * Like {@link #send(String, Object)} but specifying {@code options} that can be used to configure the delivery.
   *
   * @param address  the address to send it to
   * @param message  the message, may be {@code null}
   * @param options  delivery options
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClient send(String address, Object message, DeliveryOptions options) {
    return send(address, message, options, null);
  }

  /**
   * Like {@link #send(String, Object, DeliveryOptions)} but specifying a {@code replyHandler} that will be called if the recipient
   * subsequently replies to the message.
   *
   * @param address  the address to send it to
   * @param message  the message, may be {@code null}
   * @param options  delivery options
   * @param replyHandler  reply handler will be called when any reply from the recipient is received, may be {@code null}
   * @return a reference to this, so the API can be used fluently
   */
  public <T> EventBusClient send(String address, Object message, DeliveryOptions options, final Handler<AsyncResult<Message<T>>> replyHandler) {
    final String replyAddr;
    if (replyHandler != null) {
      final AtomicBoolean registered = new AtomicBoolean(true);
      replyAddr = UUID.randomUUID().toString();

      MessageHandler<T> messageHandler = new MessageHandler<T>() {
        @Override
        public String address() { return replyAddr; };
        @Override
        public void handleMessage(Message<T> msg) {
          if (registered.compareAndSet(true, false)) {
            this.cancelTimeout();
            unregister(this);
            replyHandler.handle(new AsyncResult<>(msg, null));
          }
        }
        @Override
        public void handleError(Throwable err) {
          if (registered.compareAndSet(true, false)) {
            this.cancelTimeout();
            unregister(this);
            replyHandler.handle(new AsyncResult<Message<T>>(null, err));
          }
        }
      };

      messageHandler.setTimeout(group.next().schedule(new Runnable() {
        @Override
        public void run() {
          messageHandler.handleError(new TimeoutException());
        }
      }, options.getSendTimeout(), TimeUnit.MILLISECONDS));

      register(messageHandler);
    } else {
      replyAddr = null;
    }
    sendOrPublish(address, message, options.getHeaders(), replyAddr, true);
    return this;
  }

  /**
   * Publish a message.<p>
   * The message will be delivered to all handlers registered to the address.
   *
   * @param address  the address to publish it to
   * @param message  the message, may be {@code null}
   * @return a reference to this, so the API can be used fluently
   *
   */
  public EventBusClient publish(String address, Object message) {
    sendOrPublish(address, message, null, null, false);
    return this;
  }

  /**
   * Create a consumer and register it against the specified address.
   *
   * @param address  the address that will register it at
   * @param handler  the handler that will process the received messages
   *
   * @return the event bus message consumer
   */
  public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
    MessageConsumer<T> consumer = new MessageConsumer<>(this, address, handler);
    register(consumer.handler());
    return consumer;
  }

  void register(MessageHandler<?> handler) {
    String address = handler.address();
    HandlerList result = consumerMap.compute(address, (k, v) -> {
      if (v == null) {
        return new HandlerList(Collections.singletonList(handler));
      } else {
        ArrayList<MessageHandler> l = new ArrayList<>(v.handlers);
        l.add(handler);
        return new HandlerList(l);
      }
    });
    if (result.handlers.size() == 1) {

      // If we would just create a task for it, that would be send upon connection creation redundandly to all other re-registered handlers
      if(!connected) {
        initializeTransport();
        connectTransport();
        return;
      }

      sendRegistration(address);
    }
  }

  void sendRegistration(String address)
  {
    Map<String, Object> obj = new HashMap<>();
    obj.put("type", "register");
    obj.put("address", address);
    final String msg = codec.encode(obj);
    send(msg);
  }

  void unregister(MessageHandler<?> handler) {
    String address = handler.address();
    HandlerList result = consumerMap.compute(address, (k, v) -> {
      if (v.handlers.size() == 1) {
        if (v.handlers.get(0) == handler) {
          return null;
        } else {
          return v;
        }
      } else {
        ArrayList<MessageHandler> list = new ArrayList<>(v.handlers);
        list.remove(handler);
        return new HandlerList(list);
      }
    });
    if (result == null) {
      // Add to queue even if disconnected, as otherwise another queued registration message might be send (and not negated by this message) after connect
      Map<String, Object> obj = new HashMap<>();
      obj.put("type", "unregister");
      obj.put("address", address);
      final String msg = codec.encode(obj);
      send(msg);
    }
  }

  /**
   * Set a connected handler, which is called everytime the connection is (re)established.
   *
   * The close handler is being handed over a Future, which it must complete in order for any queued messages
   * to be flushed. This allows the client to perform any initializing operations such as authorization before
   * sending other messages to the server.
   *
   * @param connectedHandler the connected handler
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClient connectedHandler(Handler<Handler<Void>> connectedHandler) {
    this.connectedHandler = connectedHandler;
    return this;
  }

  /**
   * Set a default exception handler.
   *
   * @param exceptionHandler the exception handler
   * @return a reference to this, so the API can be used fluently
   */
  public EventBusClient exceptionHandler(Handler<Throwable> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
    this.transport.setExceptionHandler(exceptionHandler);
    return this;
  }

  /**
   * Set a default close handler.
   *
   * @param closeHandler the close handler
   * @return a reference to this, so the API can be used fluently
   */

  public synchronized EventBusClient closeHandler(Handler<Void> closeHandler) {
    this.closeHandler = closeHandler;
    return this;
  }

  private void handleError(String message, Throwable t) {
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

  private void sendOrPublish(String address, Object body, Map<String, String> headers, String replyAddress, boolean send) {
    Map<String, Object> obj = new HashMap<>();
    obj.put("type", send ? "send" : "publish");
    obj.put("address", address);
    if (replyAddress != null) {
      obj.put("replyAddress", replyAddress);
    }
    if (headers != null) {
      obj.put("headers", headers);
    }
    obj.put("body", body);
    final String msg = codec.encode(obj);
    execute(new Handler<Transport>() {
      @Override
      public void handle(Transport event) {
        logger.info("Sending message: " + msg);
        transport.send(msg);
      }
    });
  }

  private void send(final String message) {
    execute(new Handler<Transport>() {
      @Override
      public void handle(Transport event) {
        logger.info("Sending message: " + message);
        transport.send(message);
      }
    });
  }

  private class HandlerList {

    private final List<MessageHandler> handlers;

    public HandlerList(List<MessageHandler> handlers) {
      this.handlers = handlers;
    }

    void send(Message message) {
      for (MessageHandler handler : handlers) {
        try {
          handler.handleMessage(message);
        } catch (Throwable t) {
          handleError("Exception in message handler.", t);
        }
      }
    }

    void fail(Throwable cause) {
      for (MessageHandler handler : handlers) {
        try {
          handler.handleError(cause);
        } catch (Throwable t) {
          handleError("Exception in message error handler.", t);
        }
      }
    }
  }
}
