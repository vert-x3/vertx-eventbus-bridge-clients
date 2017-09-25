package io.vertx.ext.eventbus.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.ext.eventbus.client.json.GsonCodec;
import io.vertx.ext.eventbus.client.json.JsonCodec;
import io.vertx.ext.eventbus.client.options.TcpTransportOptions;
import io.vertx.ext.eventbus.client.options.WebSocketTransportOptions;
import io.vertx.ext.eventbus.client.transport.TcpTransport;
import io.vertx.ext.eventbus.client.transport.Transport;
import io.vertx.ext.eventbus.client.transport.WebSocketTransport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventBusClient {

  private static final DeliveryOptions DEFAULT_OPTIONS = new DeliveryOptions();

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

  private final Transport transport;
  private final NioEventLoopGroup group = new NioEventLoopGroup(1);
  private Bootstrap bootstrap;
  private final EventBusClientOptions eventBusClientOptions;
  private final JsonCodec codec;

  private final ConcurrentMap<String, HandlerList> consumerMap = new ConcurrentHashMap<>();
  private ScheduledFuture<?> pingPeriodic;
  private ChannelFuture connectFuture;
  private boolean connected;

  private Handler<Handler<Void>> connectedHandler;
  private volatile Handler<Throwable> exceptionHandler;
  private Handler<Void> closeHandler;

  public EventBusClient(Transport transport, EventBusClientOptions eventBusClientOptions, JsonCodec jsonCodec) {
    this.transport = transport;
    this.bootstrap = new Bootstrap().group(this.group);
    this.eventBusClientOptions = eventBusClientOptions;
    this.codec = jsonCodec;
  }

  private ArrayDeque<Handler<Transport>> pendingTasks = new ArrayDeque<>();

  private synchronized void execute(Handler<Transport> task) {
    if (connected) {
      task.handle(transport);
    } else {
      pendingTasks.add(task);
      if (connectFuture == null) {
        transport.connectedHandler(new Handler<Void>() {
          @Override
          public void handle(Void v) {
            synchronized (EventBusClient.this) {
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
              if (closeHandler != null) {
                closeHandler.handle(null);
              }
              pingPeriodic.cancel(false);
            }
          }
        });
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(transport);
        connectFuture = bootstrap.connect(EventBusClient.this.eventBusClientOptions.getHost(), EventBusClient.this.eventBusClientOptions.getPort());
      }
    }
  }

  private void handlePendingTasks()
  {
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

  public  void connect() {
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
    send(address, message, DEFAULT_OPTIONS, null);
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
    return send(address, message, DEFAULT_OPTIONS, replyHandler);
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
      long delay = options.getSendTimeout();
      final ScheduledFuture<?> timeout = group.next().schedule(new Runnable() {
        @Override
        public void run() {
          if (registered.compareAndSet(true, false)) {
            replyHandler.handle(new AsyncResult<Message<T>>(null, new TimeoutException()));
          }
        }
      }, delay, TimeUnit.MILLISECONDS);
      replyAddr = UUID.randomUUID().toString();
      register(new MessageHandler<T>() {
        @Override
        public String address() {
          return replyAddr;
        }
        @Override
        public void handleMessage(Message<T> msg) {
          if (registered.compareAndSet(true, false)) {
            timeout.cancel(false);
            unregister(this);
            replyHandler.handle(new AsyncResult<>(msg, null));
          }
        }
        @Override
        public void handleError(Throwable err) {
          if (registered.compareAndSet(true, false)) {
            timeout.cancel(false);
            unregister(this);
            replyHandler.handle(new AsyncResult<Message<T>>(null, err));
          }
        }
      });
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

  private void register(MessageHandler<?> handler) {
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
      Map<String, Object> obj = new HashMap<>();
      obj.put("type", "register");
      obj.put("address", address);
      final String msg = codec.encode(obj);
      send(msg);
    }
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

  public void close() {
    // Todo
  }

  private void handleError(Throwable t) {
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
        transport.send(msg);
      }
    });
  }

  private void send(final String message) {
    execute(new Handler<Transport>() {
      @Override
      public void handle(Transport event) {
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
          handleError(t);
        }
      }
    }

    void fail(Throwable cause) {
      for (MessageHandler handler : handlers) {
        try {
          handler.handleError(cause);
        } catch (Throwable t) {
          handleError(t);
        }
      }
    }
  }
}
