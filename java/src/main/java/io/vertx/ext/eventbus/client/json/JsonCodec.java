package io.vertx.ext.eventbus.client.json;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class JsonCodec {

  public abstract String encode(Object obj);

  public abstract <T> T decode(String json, Class<T> type);

}
