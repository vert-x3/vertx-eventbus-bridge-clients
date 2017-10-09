package io.vertx.ext.eventbus.client.json;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class JsonCodec {

  /**
   * Creates a JSON string representation of the provided {@code obj}.
   *
   * @param obj the object to encode to JSON
   * @return the JSON string representation of {@code obj}
   */
  public abstract String encode(Object obj);

  /**
   * Creates an object based of a JSON string representation.
   *
   * @param json the JSON string
   * @param type the class of the desired return type
   * @param <T>  the desired return type
   * @return the created object
   */
  public abstract <T> T decode(String json, Class<T> type);
}
