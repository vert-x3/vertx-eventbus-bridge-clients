package io.vertx.ext.eventbus.client.json;

import com.google.gson.Gson;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GsonCodec extends JsonCodec {

  private Gson gson = new Gson();

  @Override
  public String encode(Object src) {
    return gson.toJson(src);
  }

  @Override
  public <T> T decode(String json, Class<T> type) {
    return gson.fromJson(json, type);
  }
}
