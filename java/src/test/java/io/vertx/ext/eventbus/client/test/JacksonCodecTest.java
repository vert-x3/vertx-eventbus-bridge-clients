package io.vertx.ext.eventbus.client.test;

import io.vertx.ext.eventbus.client.json.JacksonCodec;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonCodecTest extends GsonCodecTest {

  public JacksonCodecTest() {
    codec = new JacksonCodec();
  }
}
