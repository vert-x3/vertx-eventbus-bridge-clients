package io.vertx.ext.eventbus.client.options;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public class PemTrustOptions extends TrustOptions {

  public PemTrustOptions(String path)
  {
    super("PEM", path, null);
  }
}
