package io.vertx.ext.eventbus.client.logging;

public interface Logger {
  boolean isTraceEnabled();
  boolean isDebugEnabled();
  boolean isInfoEnabled();
  void trace(Object message);
  void trace(Object message, Object... params);
  void trace(Object message, Throwable t);
  void trace(Object message, Throwable t, Object... params);
  void debug(Object message);
  void debug(Object message, Object... params);
  void debug(Object message, Throwable t);
  void debug(Object message, Throwable t, Object... params);
  void info(Object message);
  void info(Object message, Object... params);
  void info(Object message, Throwable t);
  void info(Object message, Throwable t, Object... params);
  void warn(Object message);
  void warn(Object message, Object... params);
  void warn(Object message, Throwable t);
  void warn(Object message, Throwable t, Object... params);
  void error(Object message);
  void error(Object message, Object... params);
  void error(Object message, Throwable t);
  void error(Object message, Throwable t, Object... params);
  void fatal(Object message);
  void fatal(Object message, Throwable t);
}
