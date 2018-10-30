package io.vertx.ext.eventbus.client.test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.eventbus.client.json.GsonCodec;
import io.vertx.ext.eventbus.client.json.JsonCodec;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GsonCodecTest {

  JsonCodec codec = new GsonCodec();

  @Test
  public void testEncodeMap() {
    String s = codec.encode(Collections.singletonMap("string", "string_value"));
    JsonObject json = new JsonObject(s);
    assertEquals("string_value", json.getString("string"));
  }

  @Test
  public void testDecodeMap() {
    Map o = codec.decode(new JsonObject()
      .put("string", "string_value")
      .put("nested", new JsonObject().put("string", "another_string_value"))
      .encode(), Map.class);
    assertEquals(2, o.size());
    assertEquals("string_value", o.get("string"));
    Map nested = (Map) o.get("nested");
    assertEquals("another_string_value", nested.get("string"));
  }
}
