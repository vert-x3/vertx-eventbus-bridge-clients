package io.vertx.ext.eventbus.client.logging.impl;

import io.vertx.ext.eventbus.client.logging.Logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;

class JULLogger implements Logger {
  private final java.util.logging.Logger logger;

  JULLogger(final String name) {
    logger = java.util.logging.Logger.getLogger(name);
  }

  public boolean isTraceEnabled() {
    return logger.isLoggable(Level.FINEST);
  }

  public boolean isDebugEnabled() {
    return logger.isLoggable(Level.FINE);
  }

  public boolean isInfoEnabled() {
    return logger.isLoggable(Level.INFO);
  }

  public void trace(final Object message) {
    log(Level.FINEST, message);
  }

  public void trace(Object message, Object... params) {
    log(Level.FINEST, message, null, params);
  }

  public void trace(final Object message, final Throwable t) {
    log(Level.FINEST, message, t);
  }

  public void trace(Object message, Throwable t, Object... params) {
    log(Level.FINEST, message, t, params);
  }

  public void debug(final Object message) {
    log(Level.FINE, message);
  }

  public void debug(Object message, Object... params) {
    log(Level.FINE, message, null, params);
  }

  public void debug(final Object message, final Throwable t) {
    log(Level.FINE, message, t);
  }

  public void debug(Object message, Throwable t, Object... params) {
    log(Level.FINE, message, t, params);
  }

  public void info(final Object message) {
    log(Level.INFO, message);
  }

  public void info(Object message, Object... params) {
    log(Level.INFO, message, null, params);
  }

  public void info(final Object message, final Throwable t) {
    log(Level.INFO, message, t);
  }

  public void info(Object message, Throwable t, Object... params) {
    log(Level.INFO, message, t, params);
  }

  public void warn(final Object message) {
    log(Level.WARNING, message);
  }

  public void warn(Object message, Object... params) {
    log(Level.WARNING, message, null, params);
  }

  public void warn(final Object message, final Throwable t) {
    log(Level.WARNING, message, t);
  }

  public void warn(Object message, Throwable t, Object... params) {
    log(Level.WARNING, message, t, params);
  }

  public void error(final Object message) {
    log(Level.SEVERE, message);
  }

  public void error(Object message, Object... params) {
    log(Level.SEVERE, message, null, params);
  }

  public void error(final Object message, final Throwable t) {
    log(Level.SEVERE, message, t);
  }

  public void error(Object message, Throwable t, Object... params) {
    log(Level.SEVERE, message, t, params);
  }

  public void fatal(final Object message) {
    log(Level.SEVERE, message);
  }

  public void fatal(final Object message, final Throwable t) {
    log(Level.SEVERE, message, t);
  }

  private void log(Level level, Object message) {
    log(level, message, null);
  }

  private void log(Level level, Object message, Throwable t, Object... params) {
    if (!logger.isLoggable(level)) {
      return;
    }
    String msg = (message == null) ? "NULL" : message.toString();
    LogRecord record = new LogRecord(level, msg);
    record.setLoggerName(logger.getName());
    if (t != null) {
      record.setThrown(t);
    } else if (params != null && params.length != 0 && params[params.length - 1] instanceof Throwable) {
      // The exception may be the last parameters (SLF4J uses this convention).
      record.setThrown((Throwable) params[params.length - 1]);
    }
    // This will disable stack trace lookup inside JUL. If someone wants location info, they can use their own formatter
    // or use a different logging framework like sl4j, or log4j
    record.setSourceClassName(null);
    record.setParameters(params);
    logger.log(record);
  }

  private void log(Level level, Object message, Throwable t) {
    log(level, message, t, (Object[]) null);
  }
}
