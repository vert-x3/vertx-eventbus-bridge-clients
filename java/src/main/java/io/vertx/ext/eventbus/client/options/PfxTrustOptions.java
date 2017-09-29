package io.vertx.ext.eventbus.client.options;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public class PfxTrustOptions extends TrustOptions {

  public PfxTrustOptions(String path, String password)
  {
    super("PFX", path, password);
  }
}
