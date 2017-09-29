package io.vertx.ext.eventbus.client.options;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public abstract class TrustOptions {

  protected final String algorithm;
  protected final String path;
  protected final String password;

  public TrustOptions(String algorithm, String path, String password)
  {
    this.algorithm = algorithm;
    this.path = path;
    this.password = password;
  }

  public String getAlgorithm()
  {
    return this.algorithm;
  }

  public String getPath()
  {
    return this.path;
  }

  public String getPassword()
  {
    return this.password;
  }
}
