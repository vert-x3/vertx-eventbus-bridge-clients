package io.vertx.ext.eventbus.client.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonCodec extends JsonCodec {

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String encode(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  @Override
  public <T> T decode(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }
}
