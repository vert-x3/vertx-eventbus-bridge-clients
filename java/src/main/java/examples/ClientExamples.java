package examples;

import io.vertx.docgen.Source;
import io.vertx.ext.eventbus.client.AsyncResult;
import io.vertx.ext.eventbus.client.EventBusClient;
import io.vertx.ext.eventbus.client.Handler;
import io.vertx.ext.eventbus.client.Message;

@Source
public class ClientExamples {

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
}
