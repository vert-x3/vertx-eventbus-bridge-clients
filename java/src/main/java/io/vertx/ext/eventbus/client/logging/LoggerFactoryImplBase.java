package io.vertx.ext.eventbus.client.logging;

public interface LoggerFactoryImplBase {
  Logger createLogger(String name);
}
