package io.vertx.ext.eventbus.client.logging.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import static org.slf4j.spi.LocationAwareLogger.*;

class SLF4JLogger implements io.vertx.ext.eventbus.client.logging.Logger {

  private static final String FQCN = io.vertx.ext.eventbus.client.logging.Logger.class.getCanonicalName();
  private final Logger logger;

  SLF4JLogger(final String name) {
    logger = LoggerFactory.getLogger(name);
  }

  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public void trace(final Object message) {
    log(TRACE_INT, message);
  }

  public void trace(Object message, Object... params) {
    log(TRACE_INT, message, null, params);
  }

  public void trace(final Object message, final Throwable t) {
    log(TRACE_INT, message, t);
  }

  public void trace(Object message, Throwable t, Object... params) {
    log(TRACE_INT, message, t, params);
  }

  public void debug(final Object message) {
    log(DEBUG_INT, message);
  }

  public void debug(final Object message, final Object... params) {
    log(DEBUG_INT, message, null, params);
  }

  public void debug(final Object message, final Throwable t) {
    log(DEBUG_INT, message, t);
  }

  public void debug(final Object message, final Throwable t, final Object... params) {
    log(DEBUG_INT, message, t, params);
  }

  public void info(final Object message) {
    log(INFO_INT, message);
  }

  public void info(Object message, Object... params) {
    log(INFO_INT, message, null, params);
  }

  public void info(final Object message, final Throwable t) {
    log(INFO_INT, message, t);
  }

  public void info(Object message, Throwable t, Object... params) {
    log(INFO_INT, message, t, params);
  }

  public void warn(final Object message) {
    log(WARN_INT, message);
  }

  public void warn(Object message, Object... params) {
    log(WARN_INT, message, null, params);
  }

  public void warn(final Object message, final Throwable t) {
    log(WARN_INT, message, t);
  }

  public void warn(Object message, Throwable t, Object... params) {
    log(WARN_INT, message, t, params);
  }

  public void error(final Object message) {
    log(ERROR_INT, message);
  }

  public void error(Object message, Object... params) {
    log(ERROR_INT, message, null, params);
  }

  public void error(final Object message, final Throwable t) {
    log(ERROR_INT, message, t);
  }

  public void error(Object message, Throwable t, Object... params) {
    log(ERROR_INT, message, t, params);
  }

  public void fatal(final Object message) {
    log(ERROR_INT, message);
  }

  public void fatal(final Object message, final Throwable t) {
    log(ERROR_INT, message, t);
  }

  private void log(int level, Object message) {
    log(level, message, null);
  }

  private void log(int level, Object message, Throwable t) {
    log(level, message, t, (Object[]) null);
  }

  private void log(int level, Object message, Throwable t, Object... params) {
    String msg = (message == null) ? "NULL" : message.toString();

    // We need to compute the right parameters.
    // If we have both parameters and an error, we need to build a new array [params, t]
    // If we don't have parameters, we need to build a new array [t]
    // If we don't have error, it's just params.
    Object[] parameters = params;
    if (params != null  && t != null) {
      parameters = new Object[params.length + 1];
      System.arraycopy(params, 0, parameters, 0, params.length);
      parameters[params.length] = t;
    } else if (params == null  && t != null) {
      parameters = new Object[] {t};
    }

    if (logger instanceof LocationAwareLogger) {
      LocationAwareLogger l = (LocationAwareLogger) logger;
      l.log(null, FQCN, level, msg, parameters, t);
    } else {
      switch (level) {
        case TRACE_INT:
          logger.trace(msg, parameters);
          break;
        case DEBUG_INT:
          logger.debug(msg, parameters);
          break;
        case INFO_INT:
          logger.info(msg, parameters);
          break;
        case WARN_INT:
          logger.warn(msg, parameters);
          break;
        case ERROR_INT:
          logger.error(msg, parameters);
          break;
        default:
          throw new IllegalArgumentException("Unknown log level " + level);
      }
    }
  }
}
