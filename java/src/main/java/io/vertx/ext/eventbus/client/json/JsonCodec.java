package io.vertx.ext.eventbus.client.json;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class JsonCodec {

  /**
   * The default {@code JsonCodec} instance.
   *
   * <p> First {@code GsonCodec} is attempted to be loaded, when it fails then {@code JacksonCodec}
   * is attempted to be loaded. When both fails, the default instance is {@code null}.
   *
   * <p> Loading failures can be retrieved by {@link #loadingFailures()}
   */
  public static final JsonCodec DEFAULT;
  private static final List<Throwable> FAILURES;

  static {
    List<Throwable> failures = new ArrayList<Throwable>();
    JsonCodec codec = null;
    try {
      codec = new GsonCodec();
    } catch (Throwable e1) {
      failures.add(e1);
      try {
        codec = new JacksonCodec();
      } catch (Throwable e2) {
        failures.add(e2);
      }
    }
    DEFAULT = codec;
    FAILURES = failures;
  }

  /**
   * @return the failures encountered while loading the default codec.
   */
  public static List<Throwable> loadingFailures() {
    return FAILURES;
  }

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
