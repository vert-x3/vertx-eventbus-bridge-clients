package io.vertx.eventbusclient.test;

import io.vertx.eventbusclient.json.JacksonCodec;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonCodecTest extends GsonCodecTest {

  public JacksonCodecTest() {
    codec = new JacksonCodec();
  }
}
