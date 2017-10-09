package io.vertx.ext.eventbus.client.options;

import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public abstract class TrustOptions {

  protected final String path;
  protected final String password;

  public TrustOptions(String path, String password)
  {
    this.path = path;
    this.password = password;
  }

  public abstract KeyStore getKeyStore() throws Exception;

  protected KeyStore getSupportedKeyStore(String algorithm) throws Exception
  {
    KeyStore keyStore = KeyStore.getInstance(algorithm);

    if(this.password != null) {
      keyStore.load(new FileInputStream(this.path), this.password.toCharArray());
    } else {
      keyStore.load(new FileInputStream(this.path), null);
    }

    return keyStore;
  }
}
