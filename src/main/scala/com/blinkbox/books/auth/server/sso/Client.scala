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
  def unitRequest(req: HttpRequest): Future[Unit]
  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest): Future[T]
  def withCredentials(credentials: HttpCredentials): Client
}

trait SprayClient extends Client {
  val config: SSOConfig
  val system: ActorSystem
  val ec: ExecutionContext
  lazy val credentials: HttpCredentials = config.credentials

  implicit lazy val _timeout = Timeout(config.timeout)
  implicit lazy val _system = system
  implicit lazy val _ec = ec

  protected def doSendReceive(transport: ActorRef): HttpRequest => Future[HttpResponse] = sendReceive(transport)

  protected lazy val unitIfSuccessful = { resp: HttpResponse =>
    if (resp.status.isSuccess) () else throw new UnsuccessfulResponseException(resp)
  }

  protected lazy val basePipeline: Future[SendReceive] = for {
    Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(config.host, port = config.port, sslEncryption = true)
  } yield {
    addCredentials(credentials) ~>
    addHeader("X-CSRF-Protection", "Foobar") ~> // This is needed for some obscure reason (see SSO API doc.)
    doSendReceive(connector)
  }

  protected lazy val unitPipeline = basePipeline map { _ ~> unitIfSuccessful }

  protected def dataPipeline[T : FromResponseUnmarshaller] = basePipeline map { _ ~> unmarshal[T] }

  def unitRequest(req: HttpRequest): Future[Unit] = unitPipeline.flatMap(_(req))

  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest): Future[T] = dataPipeline[T].flatMap(_(req))

  def withCredentials(creds: HttpCredentials): Client = new SprayClient {
    override val config: SSOConfig = SprayClient.this.config
    override val ec: ExecutionContext = SprayClient.this.ec
    override val system: ActorSystem = SprayClient.this.system
    override lazy val credentials = creds
    override def unitRequest(req: HttpRequest) = SprayClient.this.unitRequest(req)
    override def dataRequest[T: FromResponseUnmarshaller](req: HttpRequest) = SprayClient.this.dataRequest[T](req)
  }
}

class DefaultClient(val config: SSOConfig)(implicit val ec: ExecutionContext, val system: ActorSystem) extends SprayClient
