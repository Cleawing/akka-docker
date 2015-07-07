package com.cleawing.docker.api

import com.cleawing.akka.http.Client
import com.cleawing.akka.http.TLSSupport
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Promise, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{StreamTcpException, ActorMaterializer}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{StatusCode, HttpRequest, IllegalResponseException, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.cleawing.docker.api.Data.Implicits._
import org.json4s.jackson.JsonMethods.parse

object RemoteClient {
  val config = ConfigFactory.load()

  def apply()(implicit system: ActorSystem) : RemoteClient = {
    val port = if (config.getBoolean("docker.tls")) config.getInt("docker.tlsPort") else config.getInt("docker.port")
    apply(config.getString("docker.host"), port, config.getBoolean("docker.tls"))
  }

  def apply(tlsOn: Boolean)(implicit system: ActorSystem) : RemoteClient = {
    val port = if (tlsOn) config.getInt("docker.tlsPort") else config.getInt("docker.port")
    apply(config.getString("docker.host"), port, tlsOn)
  }

  def apply(tlsOn: Boolean, tls: Option[TLSSupport])(implicit system: ActorSystem) : RemoteClient = {
    val port = if (tlsOn) config.getInt("docker.tlsPort") else config.getInt("docker.port")
    apply(config.getString("docker.host"), port, tlsOn, tls)
  }

  def apply(host: String)(implicit system: ActorSystem) : RemoteClient = {
    val port = if (config.getBoolean("docker.tls")) config.getInt("docker.tlsPort") else config.getInt("docker.port")
    apply(host, port, config.getBoolean("docker.tls"))
  }

  def apply(host: String, tlsOn: Boolean)(implicit system: ActorSystem) : RemoteClient = {
    val port = if (tlsOn) config.getInt("docker.tlsPort") else config.getInt("docker.port")
    apply(host, port, tlsOn)
  }

  def apply(port: Int)(implicit system: ActorSystem) : RemoteClient = {
    apply(config.getString("docker.host"), port, config.getBoolean("docker.tls"))
  }

  def apply(port: Int, tlsOn: Boolean)(implicit system: ActorSystem) : RemoteClient = {
    apply(config.getString("docker.host"), port, tlsOn)
  }


  def apply(host: String, port: Int)(implicit system: ActorSystem) : RemoteClient = {
    new RemoteClient(host, port, config.getBoolean("docker.tls"))
  }

  def apply(host: String, port: Int, tlsOn: Boolean)(implicit system: ActorSystem) : RemoteClient = {
    apply(host, port, tlsOn, if (tlsOn) Some(TLSSupport(config.getString("docker.cert_path"))) else None)
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

class RemoteClient(
  val host: String, val port: Int, val tlsOn: Boolean,
  val tlsSupport: Option[TLSSupport] = None)
  (implicit val system: ActorSystem) extends Client {

  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val logger = Logging(system, getClass)

  def ping() : Future[Either[Data.Error, Data.Pong]] = {
    simpleGet("/_ping").map {
      case Right(success: RemoteClient.Success) => Right(Data.Pong(success.body))
      case Right(error: RemoteClient.Error) => Left(Data.UnexpectedError(error.body))
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  def version() : Future[Either[Data.Error, Data.Version]] = {
    simpleGet("/version").map {
      case Right(success: RemoteClient.Success) => Right(parse(success.body).extract[Data.Version])
      case Right(error: RemoteClient.Error) => Left(Data.UnexpectedError(error.body))
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  def info() : Future[Either[Data.Error, Data.Info]] = {
    simpleGet("/info").map {
      case Right(success: RemoteClient.Success) => Right(parse(success.body).extract[Data.Info])
      case Right(error: RemoteClient.Error) => Left(Data.UnexpectedError(error.body))
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  // FIXME. Handle parameters
  def images() : Future[Either[Data.Error, Data.Images]] = {
    simpleGet("/images/json").map {
      case Right(success: RemoteClient.Success) => Right(Data.Images(parse(success.body).extract[Seq[Data.Internals.Image]]))
      case Right(error: RemoteClient.Error) => Left(Data.UnexpectedError(error.body))
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  def imageHistory(id: String) : Future[Either[Data.Error, Data.ImageHistory]] = {
    simpleGet(s"/images/$id/history").map {
      case Right(success: RemoteClient.Success) => Right(Data.ImageHistory(parse(success.body).extract[Seq[Data.Internals.History]]))
      case Right(error: RemoteClient.Error) => error.status match {
        case NotFound => Left(Data.NotFound(error.body))
        case _ => Left(Data.UnexpectedError(error.body))
      }
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  // FIXME. Handle X-Registry-Auth header
//  def imageCreate(fromImage: Option[String],
//                  fromSrc: Option[String],
//                  repo: Option[String],
//                  tag: Option[String],
//                  registry: Option[String]) : Future[Either[Data.Error, Data.ImageHistory]] = {
//
//  }

  // FIXME. Handle parameters
  def containers() : Future[Either[Data.Error, Data.Containers]] = {
    simpleGet("/containers/json").map {
      case Right(success: RemoteClient.Success) => Right(Data.Containers(parse(success.body).extract[Seq[Data.Internals.Container]]))
      case Right(error: RemoteClient.Error) => error.status match {
        case BadRequest => Left(Data.BadParameter(error.body))
        case InternalServerError => Left(Data.ServerError(error.body))
        case _ => Left(Data.UnexpectedError(error.body))
      }
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  def container_inspect(id: String) : Future[Either[Data.Error, Data.ContainerInspect]] = {
    simpleGet(s"/containers/$id/json").map {
      case Right(success: RemoteClient.Success) => Right(parse(success.body).extract[Data.ContainerInspect])
      case Right(error: RemoteClient.Error) => error.status match {
        case NotFound => Left(Data.NotFound(error.body))
        case _ => Left(Data.UnexpectedError(error.body))
      }
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  // FIXME. Args: ps_args
  def container_top(id: String) : Future[Either[Data.Error, Data.ContainerTop]] = {
    simpleGet(s"/containers/$id/top").map {
      case Right(success: RemoteClient.Success) => Right(parse(success.body).extract[Data.ContainerTop])
      case Right(error: RemoteClient.Error) => error.status match {
        case NotFound => Left(Data.NotFound(error.body))
        case _ => Left(Data.UnexpectedError(error.body))
      }
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  def container_changes(id: String) : Future[Either[Data.Error, Data.ContainerChanges]] = {
    simpleGet("/containers/json").map {
      case Right(success: RemoteClient.Success) => Right(Data.ContainerChanges(parse(success.body).extract[Seq[Data.Internals.ContainerChange]]))
      case Right(error: RemoteClient.Error) => error.status match {
        case NotFound => Left(Data.NotFound(error.body))
        case InternalServerError => Left(Data.ServerError(error.body))
        case _ => Left(Data.UnexpectedError(error.body))
      }
      case Left(ex : RemoteClient.Failure) => Left(processFailure(ex))
    }
  }

  private def simpleGet(uri: String) : Future[Either[RemoteClient.Failure, RemoteClient.Response]] = {
    doRequest(RequestBuilding.Get(uri))
  }

  private def doRequest(request: HttpRequest) : Future[Either[RemoteClient.Failure, RemoteClient.Response]] = {
    val promise = Promise[Either[RemoteClient.Failure, RemoteClient.Response]]()

    singleRequest(request).onComplete {
      case scala.util.Success(response) => response.status match {
        case OK => processSuccess(response).map(promise.success(_))
        case other => processError(response).map(promise.success(_))
      }
      case scala.util.Failure(ex) => ex match {
        case _: StreamTcpException | _: IllegalResponseException => promise.success(Left(RemoteClient.Failure(ex)))
        case t: Throwable => promise.failure(t)
      }
    }

    promise.future
  }

  private def processSuccess(response: HttpResponse) = {
    Unmarshal(response.entity).to[String].map(body => Right(RemoteClient.Success(response.status, body)))
  }

  private def processError(response: HttpResponse) = {
    Unmarshal(response.entity).to[String].map(body => Right(RemoteClient.Error(response.status, body)))
  }

  private def processFailure(ex: RemoteClient.Failure) : Data.Failure = {
    ex.cause match {
      case _: StreamTcpException | _: IllegalResponseException => Data.ConnectionFailed(ex.cause)
    }
  }
}
