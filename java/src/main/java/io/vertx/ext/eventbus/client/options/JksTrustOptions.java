package io.vertx.ext.eventbus.client.options;

import java.security.KeyStore;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public class JksTrustOptions extends TrustOptions {

  public JksTrustOptions(String path, String password)
  {
    super(path, password);
  }

  public KeyStore getKeyStore() throws Exception
  {
    return this.getSupportedKeyStore("jks");
  }
}
