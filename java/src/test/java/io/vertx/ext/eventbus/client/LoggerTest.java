package io.vertx.ext.eventbus.client;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.eventbus.client.logging.Logger;
import io.vertx.ext.eventbus.client.logging.LoggerFactory;
import org.junit.Test;

public class LoggerTest {

  private Logger logger = LoggerFactory.getLogger(LoggerTest.class);
  private Logger logger2 = LoggerFactory.getLogger("LoggerTest");

  @Test
  public void testLogger() {

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    Exception exception = new Exception("hello world");

    JsonObject param = new JsonObject().put("hello", "world");

    this.logger2.isTraceEnabled();
    this.logger.isDebugEnabled();
    this.logger.isInfoEnabled();
    this.logger.trace("hello world");
    this.logger.trace("hello world", param);
    this.logger.trace("hello world", exception);
    this.logger.trace("hello world", exception, param);
    this.logger.debug("hello world");
    this.logger.debug("hello world", param);
    this.logger.debug("hello world", exception);
    this.logger.debug("hello world", exception, param);
    this.logger.info("hello world");
    this.logger.info("hello world", param);
    this.logger.info("hello world", exception);
    this.logger.info("hello world", exception, param);
    this.logger.warn("hello world");
    this.logger.warn("hello world", param);
    this.logger.warn("hello world", exception);
    this.logger.warn("hello world", exception, param);
    this.logger.error("hello world");
    this.logger.error("hello world", param);
    this.logger.error("hello world", exception);
    this.logger.error("hello world", exception, param);
    this.logger.fatal(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME);
    this.logger.fatal("hello world", exception);

    LoggerFactory.removeLogger("LoggerTest");
  }
}
