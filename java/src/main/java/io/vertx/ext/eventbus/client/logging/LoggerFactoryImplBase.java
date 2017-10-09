package io.vertx.ext.eventbus.client.logging;

interface LoggerFactoryImplBase {
  Logger createLogger(String name);
}
