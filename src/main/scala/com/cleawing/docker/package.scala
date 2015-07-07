package com.cleawing

import java.io.File

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{HttpsContext, Http}
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source, Flow}
import com.cleawing.docker.api.{TLSSupport, RemoteClient}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, ExecutionContextExecutor}

package object docker {
  val config = ConfigFactory.load()

  trait Client {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer

    val host: String
    val port: Int
    val tls_on: Boolean
    val tls: Option[TLSSupport]

    val logger: LoggingAdapter

    lazy val dockerConnectionFlow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] = {
      if (tls_on && tls.isDefined)
        Http().outgoingConnectionTls(host = host, port = port, httpsContext = Some(HttpsContext(tls.get.getSSLContext)))
      else
        Http().outgoingConnection(host, port)
    }

    def singleRequest(request: HttpRequest): Future[HttpResponse] = {
      try {
        Source.single(request).
          via(dockerConnectionFlow).
          runWith(Sink.head)
      } catch { // Workaround. Sometimes instead of StreamTcpException("Connection failed.") we got NoSuchElementException
          case _ : NoSuchElementException => singleRequest(request)
          case t : Throwable => throw t
      }
    }
  }

  object Remote {
    def apply()(implicit system: ActorSystem) : RemoteClient = {
      val port = if (config.getBoolean("docker.tls")) config.getInt("docker.tlsPort") else config.getInt("docker.port")
      apply(config.getString("docker.host"), port, config.getBoolean("docker.tls"))
    }

    def apply(tls_on: Boolean)(implicit system: ActorSystem) : RemoteClient = {
      val port = if (tls_on) config.getInt("docker.tlsPort") else config.getInt("docker.port")
      apply(config.getString("docker.host"), port, tls_on)
    }

    def apply(tls_on: Boolean, tls: Option[TLSSupport])(implicit system: ActorSystem) : RemoteClient = {
      val port = if (tls_on) config.getInt("docker.tlsPort") else config.getInt("docker.port")
      apply(config.getString("docker.host"), port, tls_on, tls)
    }

    def apply(host: String)(implicit system: ActorSystem) : RemoteClient = {
      val port = if (config.getBoolean("docker.tls")) config.getInt("docker.tlsPort") else config.getInt("docker.port")
      apply(host, port, config.getBoolean("docker.tls"))
    }

    def apply(host: String, tls_on: Boolean)(implicit system: ActorSystem) : RemoteClient = {
      val port = if (tls_on) config.getInt("docker.tlsPort") else config.getInt("docker.port")
      apply(host, port, tls_on)
    }

    def apply(port: Int)(implicit system: ActorSystem) : RemoteClient = {
      apply(config.getString("docker.host"), port, config.getBoolean("docker.tls"))
    }

    def apply(port: Int, tls_on: Boolean)(implicit system: ActorSystem) : RemoteClient = {
      apply(config.getString("docker.host"), port, tls_on)
    }


    def apply(host: String, port: Int)(implicit system: ActorSystem) : RemoteClient = {
      new RemoteClient(host, port, config.getBoolean("docker.tls"))
    }

    def apply(host: String, port: Int, tls_on: Boolean)(implicit system: ActorSystem) : RemoteClient = {
      apply(host, port, tls_on, if (tls_on) Some(TLS()) else None)
    }

    def apply(host: String, port: Int, tls_on: Boolean, tls: Option[TLSSupport])(implicit system: ActorSystem) : RemoteClient = {
      new RemoteClient(host, port, tls_on, tls)
    }

    sealed trait Response {
      val status: StatusCode
      val body: String
    }

    case class Success(status: StatusCode, body: String) extends Response
    case class Error(status: StatusCode, body: String) extends Response
    case class Failure(cause: Throwable)
  }

  object TLS {
    def apply() : TLSSupport = {
      apply(config.getString("docker.cert_path"))
    }

    def apply(cert_path: String) : TLSSupport = {
      val path = new File(cert_path)

      if (!path.exists()) illegal(s"Path '$cert_path' is not exist")
      if (!path.isDirectory) illegal(s"Path '$cert_path' is not a directory")

      val (key, cert) = (pem(path, "key"), pem(path, "cert"))

      val ca = List(key, cert).filterNot(isFile) match {
        case List(_, _) => illegal(s"Path '$cert_path' does not contain any certs")
        case List(missed_path) => illegal(s"Path '$missed_path' is not a file")
        case List() => if (isFile(pem(path, "ca"))) Some(pem(path, "ca")) else None
      }
      apply(key, cert, ca)
    }

    def apply(keyPath: String, certPath: String, caPath: Option[String] = None) : TLSSupport = {
      apply(List(Some(keyPath), Some(certPath), caPath))
    }

    private def apply(paths: List[Option[String]]) : TLSSupport = {
      paths.collect {
        case Some(f) if !(isFile(f) && isCanRead(f)) =>
          illegal(s"Path '$f' is not readable or not a file")
      }
      paths match {
        case List(Some(keyPath), Some(certPath), caPath) => new TLSSupport(keyPath, certPath, caPath)
      }
    }

    private def pem(path: File, name: String) = s"${path.getAbsolutePath}/$name.pem"
    private def isFile(f: String) = new File(f).isFile
    private def isCanRead(f: String) = new File(f).canRead
    private def illegal(msg: String) = throw new IllegalArgumentException(msg)
  }
}
