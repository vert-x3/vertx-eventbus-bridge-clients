package io.vertx.ext.eventbus.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;
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

  public static EventBusClient tcp(int port, String host) {
    return new EventBusClient(port, host, new TcpTransport());
  }

  public static EventBusClient websocket(int port, String host) {
    return new EventBusClient(port, host, new WebSocketTransport());
  }

  private final NioEventLoopGroup group = new NioEventLoopGroup(1);
  private final ConcurrentMap<String, HandlerList> consumerMap = new ConcurrentHashMap<>();
  private final int port;
  private final String host;
  private Bootstrap bootstrap;
  private ChannelFuture connectFuture;
  private final Transport transport;
  private boolean connected;
  private Handler<Void> closeHandler;
  private ScheduledFuture<?> pingPeriodic;

  public EventBusClient(int port, String host, Transport transport) {
    this.port = port;
    this.host = host;
    this.transport = transport;
    this.bootstrap = new Bootstrap().group(group);
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
                  JsonObject msg = new JsonObject();
                  msg.addProperty("type", "ping");
                  Gson gson = new Gson();
                  send(gson.toJson(msg));
                }
              }, 100, 100, TimeUnit.MILLISECONDS);
              Handler<Transport> t;
              connected = true;
              while ((t = pendingTasks.poll()) != null) {
                t.handle(transport);
              }
            }
          }
        });
        transport.messageHandler(new Handler<String>() {
          @Override
          public void handle(String json) {
            Gson gson = new Gson();
            JsonObject msg = gson.fromJson(json, JsonObject.class);
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
        connectFuture = bootstrap.connect(host, port);
      }
    }
  }

  private void handleMsg(JsonObject msg) {
    JsonElement type = msg.get("type");
    if (type != null) {
      switch (type.getAsString()) {
        case "message":
        case "rec": {
          String address = msg.get("address").getAsString();
          if (address == null) {
            // TCP bridge that replies an error...
            return;
          }
          HandlerList consumers = consumerMap.get(address);
          if (consumers != null) {
            JsonObject body = msg.get("body").getAsJsonObject();
            Map<String, String> headers;
            JsonElement msgHeaders = msg.get("headers");
            if (msgHeaders != null) {
              headers = new HashMap<>();
              for (Map.Entry<String, JsonElement> entry : msgHeaders.getAsJsonObject().entrySet()) {
                headers.put(entry.getKey(), entry.getValue().getAsString());
              }
            } else {
              headers = Collections.emptyMap();
            }
            consumers.send(new Message(address, headers, body));
          }
          break;
        }
        case "err": {
          String address = msg.get("address").getAsString();
          String message = msg.get("message").getAsString();
          int failureCode = msg.get("failureCode").getAsInt();
          String failureType = msg.get("failureType").getAsString();
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

  public void register(MessageHandler<?> handler) {
    String address = handler.address();
    synchronized (consumerMap) {
      HandlerList consumers = consumerMap.get(address);
      List<MessageHandler> handlers;
      if (consumers == null) {
        handlers = Collections.singletonList((MessageHandler) handler);
      } else {
        ArrayList<MessageHandler> tmp = new ArrayList<>(consumers.handlers);
        tmp.add(handler);
        handlers = new ArrayList<>(tmp);
      }
      consumerMap.put(address, new HandlerList(handlers));
    }
    Gson gson = new Gson();
    JsonObject obj = new JsonObject();
    obj.addProperty("type", "register");
    obj.addProperty("address", address);
    final String msg = gson.toJson(obj);
    send(msg);
  }

  void unregister(MessageHandler<?> handler) {
    String address = handler.address();
    synchronized (consumerMap) {
      HandlerList consumers = consumerMap.get(address);
      if (consumers == null) {
        return;
      }
      if (!consumers.handlers.contains(handler)) {
        return;
      }
      List<MessageHandler> handlers = new ArrayList<>(consumers.handlers);
      handlers.remove(handler);
      if (handlers.isEmpty()) {
        consumerMap.remove(address);
      } else {
        consumerMap.put(address, new HandlerList(new ArrayList<>(handlers)));
      }
    }
    Gson gson = new Gson();
    JsonObject obj = new JsonObject();
    obj.addProperty("type", "unregister");
    obj.addProperty("address", address);
    final String msg = gson.toJson(obj);
    send(msg);
  }

  public synchronized EventBusClient closeHandler(Handler<Void> handler) {
    closeHandler = handler;
    return this;
  }

  public void close() {
    // Todo
  }

  private void sendOrPublish(String address, Object body, Map<String, String> headers, String replyAddress, boolean send) {
    Gson gson = new Gson();
    JsonObject obj = new JsonObject();
    obj.addProperty("type", send ? "send" : "publish");
    obj.addProperty("address", address);
    if (replyAddress != null) {
      obj.addProperty("replyAddress", replyAddress);
    }
    if (headers != null) {
      JsonObject h = new JsonObject();
      for (Map.Entry<String, String> header : headers.entrySet()) {
        h.addProperty(header.getKey(), header.getValue());
      }
      obj.add("headers", h);
    }
    setBody(obj, body);
    final String msg = gson.toJson(obj);
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

  private void setBody(JsonObject json, Object body) {
    if (body instanceof String) {
      json.addProperty("body", (String) body);
    } else if (body instanceof Number) {
      json.addProperty("body", (Number) body);
    } else if (body instanceof Boolean) {
      json.addProperty("body", (Character) body);
    } else if (body instanceof Character) {
      json.addProperty("body", (Character) body);
    } else if (body instanceof JsonElement) {
      json.add("body", (JsonElement) body);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static class HandlerList {

    private final List<MessageHandler> handlers;
    private long current = 0;

    public HandlerList(List<MessageHandler> handlers) {
      this.handlers = handlers;
    }

    void send(Message message) {
      int s = handlers.size();
      if (s > 0) {
        int index = (int) current++ % s;
        handlers.get(index).handleMessage(message);
      }
    }

    void publish(Message message) {
      for (MessageHandler handler : handlers) {
        handler.handleMessage(message);
      }
    }

    void fail(Throwable cause) {
      for (MessageHandler handler : handlers) {
        handler.handleError(cause);
      }
    }

  }

}
