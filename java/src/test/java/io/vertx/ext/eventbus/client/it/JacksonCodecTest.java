package io.vertx.ext.eventbus.client.it;

import io.vertx.ext.eventbus.client.json.JsonCodec;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonCodecTest {

  @Test
  public void testDefault() {
    assertNotNull(JsonCodec.DEFAULT);
    assertEquals("JacksonCodec", JsonCodec.DEFAULT.getClass().getSimpleName());
    assertEquals(1, JsonCodec.loadingFailures().size());
  }
}
