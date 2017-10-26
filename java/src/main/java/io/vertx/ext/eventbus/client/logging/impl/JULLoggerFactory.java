package io.vertx.ext.eventbus.client.logging.impl;

import io.vertx.ext.eventbus.client.logging.Logger;
import io.vertx.ext.eventbus.client.logging.LoggerFactoryImplBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class JULLoggerFactory implements LoggerFactoryImplBase {

  private static void loadConfig() {
    try (InputStream is = JULLoggerFactory.class.getClassLoader().getResourceAsStream("vertx-default-jul-logging.properties")) {
      if (is != null) {
        LogManager.getLogManager().readConfiguration(is);
      }
    } catch (IOException ignore) {
    }
  }

  static {
    // Try and load vert.x JUL default logging config from classpath
    if (System.getProperty("java.util.logging.config.file") == null) {
      loadConfig();
    }
  }

  public Logger createLogger(final String name) {
    return new JULLogger(name);
  }
}
