package com.cleawing.docker.api

import java.io.{BufferedReader, FileReader, FileInputStream}
import java.security.{KeyPair, SecureRandom, KeyStore, Security}
import java.security.cert.{CertificateFactory, Certificate}
import javax.net.ssl.{SSLContext, KeyManagerFactory, X509TrustManager, TrustManagerFactory}

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMReader

private[docker] class TLSSupport(keyPath: String, certPath: String, caPath: Option[String]) {
  Security.addProvider(new BouncyCastleProvider)

  def getSSLContext : SSLContext = {
    val ctx = SSLContext.getInstance("TLSv1")
    val trust = for {
      ca    <- caPath
      trust <- trustManager(ca)
    } yield trust

    ctx.init(keyManagers, trust.map(Array(_)).orNull, new SecureRandom)
    ctx
  }

  private def certificate(path: String): Certificate = {
    val certStm = new FileInputStream(path)
    try CertificateFactory.getInstance("X.509").generateCertificate(certStm)
    finally certStm.close()
  }

  private def withStore[T](f: KeyStore => T): KeyStore = {
    val store = KeyStore.getInstance(KeyStore.getDefaultType)
    f(store)
    store
  }

  private def keyStore = {
    // using bouncycastle b/c the provided key may not be in pkcs8 format (boot2dockers keys are not)
    // bouncycastle's PEM reader seems a bit more robust
    val key = new PEMReader(new BufferedReader(new FileReader(keyPath))).
      readObject().asInstanceOf[KeyPair].getPrivate

    withStore { store =>
      store.load(null, null)
      store.setKeyEntry("key", key, "".toCharArray, Array(certificate(certPath)))}
  }

  private def trustStore(caPath: String) = withStore { store =>
    store.load(null, null)
    store.setCertificateEntry("cacert", certificate(caPath))
  }

  private def trustManager(caPath: String) = {
    val fact = TrustManagerFactory.getInstance("SunX509", "SunJSSE")
    fact synchronized {
      fact.init(trustStore(caPath))
      fact.getTrustManagers.find(_.isInstanceOf[X509TrustManager])
    }
  }

  private def keyManagers = {
    val algo = Option(Security.getProperty("ssl.KeyManagerFactory.algorithm")).getOrElse("SunX509")
    val kmf = KeyManagerFactory.getInstance(algo)
    kmf.init(keyStore, "".toCharArray)
    kmf.getKeyManagers
  }
}
