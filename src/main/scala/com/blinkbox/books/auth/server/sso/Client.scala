package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.blinkbox.books.auth.server.SSOConfig
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.httpx.unmarshalling.FromResponseUnmarshaller

import scala.concurrent.{ExecutionContext, Future}

trait Client {
  def defaultCredentials: HttpCredentials
  def unitRequest(req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[Unit]
  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[T]
}

trait SprayClient extends Client {
  val config: SSOConfig
  val system: ActorSystem
  val ec: ExecutionContext

  implicit lazy val _timeout = Timeout(config.timeout)
  implicit lazy val _system = system
  implicit lazy val _ec = ec

  protected def doSendReceive(transport: ActorRef): HttpRequest => Future[HttpResponse] = sendReceive(transport)

  protected lazy val unitIfSuccessful = { resp: HttpResponse =>
    if (resp.status.isSuccess) () else throw new UnsuccessfulResponseException(resp)
  }

  protected def basePipeline(credentials: HttpCredentials): Future[SendReceive] = for {
    Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(config.host, port = config.port, sslEncryption = true)
  } yield {
    addCredentials(credentials) ~>
    addHeader("X-CSRF-Protection", "Foobar") ~> // see SSO API doc.
    doSendReceive(connector)
  }

  protected def unitPipeline(credentials: HttpCredentials) = basePipeline(credentials) map { _ ~> unitIfSuccessful }

  protected def dataPipeline[T : FromResponseUnmarshaller](credentials: HttpCredentials) =
    basePipeline(credentials) map { _ ~> unmarshal[T] }

  override val defaultCredentials = config.credentials

  override def unitRequest(req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[Unit] =
    unitPipeline(credentials).flatMap(_(req))

  override def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[T] =
    dataPipeline[T](credentials).flatMap(_(req))
}

class DefaultClient(val config: SSOConfig)(implicit val ec: ExecutionContext, val system: ActorSystem) extends SprayClient
