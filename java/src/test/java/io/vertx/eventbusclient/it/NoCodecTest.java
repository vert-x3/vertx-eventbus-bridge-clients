package io.vertx.eventbusclient.it;

import io.vertx.eventbusclient.EventBusClient;
import io.vertx.eventbusclient.EventBusClientOptions;
import io.vertx.eventbusclient.json.JsonCodec;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NoCodecTest {

  @Test
  public void testDefault() {
    assertNull(JsonCodec.DEFAULT);
    assertEquals(2, JsonCodec.loadingFailures().size());
  }

  @Test
  public void testFailure() {
    try {
      EventBusClient.tcp(new EventBusClientOptions());
      fail();
    } catch (IllegalStateException ignore) {
      // Expected
    }
  }
}
