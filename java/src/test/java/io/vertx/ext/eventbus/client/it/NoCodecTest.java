package io.vertx.ext.eventbus.client.it;

import io.vertx.ext.eventbus.client.EventBusClient;
import io.vertx.ext.eventbus.client.EventBusClientOptions;
import io.vertx.ext.eventbus.client.json.JsonCodec;
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
