package examples;

import io.vertx.docgen.Source;
import io.vertx.ext.eventbus.client.AsyncResult;
import io.vertx.ext.eventbus.client.EventBusClient;
import io.vertx.ext.eventbus.client.EventBusClientOptions;
import io.vertx.ext.eventbus.client.Handler;
import io.vertx.ext.eventbus.client.Message;
import io.vertx.ext.eventbus.client.MessageConsumer;

@Source
public class ClientExamples {

  public void exampleTCPCreate() {
    // Create a bus client with the default options that will connect to localhost:7000 via TCP
    EventBusClient tcpEventBusClient = EventBusClient.tcp();

    // Create a bus client with specified host and port and TLS enabled
    EventBusClientOptions options = new EventBusClientOptions()
      .setHost("127.0.0.1").setPort(7001)
      .setSsl(true)
      .setTrustStorePath("/path/to/store.jks")
      .setTrustStorePassword("change-it");
    EventBusClient sslTcpEventBusClient = EventBusClient.tcp(options);

  }

  public void exampleWebSocketCreate() {
    // Create a bug client with default options that will connect to http://localhost:80/eventbus/websocket via WebSocket
    EventBusClient webSocketEventBusClient = EventBusClient.websocket();

    // Create a bus client with specified host and port, TLS enabled and WebSocket path.
    EventBusClientOptions options = new EventBusClientOptions()
      .setHost("127.0.0.1").setPort(8043)
      .setSsl(true)
      .setTrustStorePath("/path/to/store.jks")
      .setTrustStorePassword("change-it")
      .setWebsocketPath("/eventbus/message")
      ;
    EventBusClient sslWebSocketEventBusClient = EventBusClient.websocket(options);

  }

  public void example01() {
    // Create a bus client with the default options that will connect to localhost:7000
    EventBusClient busClient = EventBusClient.tcp();

    // Send a message to the bus, this will connect the client to the server
    busClient.send("newsfeed", "Breaking news: something great happened");
  }

  public void example02(EventBusClient busClient) {
    // Send a message to the bus and expect a reply
    busClient.send("newsfeed", "Breaking news: something great happened", new Handler<AsyncResult<Message<String>>>() {
      @Override
      public void handle(AsyncResult<Message<String>> reply) {
        System.out.println("We got the reply");
      }
    });
  }

  public void example03(EventBusClient busClient) {
    // Publish a message to the bus
    busClient.publish("newsfeed", "Breaking news: something great happened");
  }

  public void example04(EventBusClient busClient) {
    // Set a consumer, this will connect the client to the server
    busClient.consumer("newsfeed", new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> message) {
        System.out.println("Received a news " + message.body());
      }
    });
  }

  public void example05(MessageConsumer<String> consumer) {
    consumer.unregister();
  }

  public void example06(EventBusClient busClient) {
    // Specify the close handler to the EventBusClient
    busClient.closeHandler(v -> System.out.println("Bus Client Closed"));
    // Closes the connection to the bridge server if it is open
    busClient.close();

  }

}
