package io.vertx.ext.eventbus.client.options;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * @author <a href="mailto:pl@linux.com">Phil Lehmann</a>
 */
public class PemTrustOptions extends TrustOptions {

  public PemTrustOptions(String path)
  {
    super(path, null);
  }

  public KeyStore getKeyStore() throws Exception
  {
    KeyStore keystore = KeyStore.getInstance("jks");
    keystore.load(null);

    String pemString = new String(Files.readAllBytes(Paths.get(this.path)));
    String[] pemParts = pemString.split("-----BEGIN CERTIFICATE-----");
    pemParts = pemParts[1].split("-----END CERTIFICATE-----");
    byte[] certificateBytes = DatatypeConverter.parseBase64Binary(pemParts[0]);

    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
    keystore.setCertificateEntry("certificate", certificate);
    return keystore;
  }
}
