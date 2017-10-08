package io.vertx.ext.eventbus.client.logging;

public class SLF4JLoggerFactory implements LoggerFactoryImplBase {

  public Logger createLogger(final String clazz) {
    return new SLF4JLogger(clazz);
  }
}
