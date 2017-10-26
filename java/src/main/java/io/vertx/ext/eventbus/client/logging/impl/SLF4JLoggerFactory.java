package io.vertx.ext.eventbus.client.logging.impl;

import io.vertx.ext.eventbus.client.logging.Logger;
import io.vertx.ext.eventbus.client.logging.LoggerFactoryImplBase;

class SLF4JLoggerFactory implements LoggerFactoryImplBase {

  public Logger createLogger(final String clazz) {
    return new SLF4JLogger(clazz);
  }
}
