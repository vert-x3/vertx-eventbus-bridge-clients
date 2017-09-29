package io.vertx.ext.eventbus.client.options;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public class JksTrustOptions extends TrustOptions {

  public JksTrustOptions(String path, String password)
  {
    super("JKS", path, password);
  }
}
