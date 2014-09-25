package com.blinkbox.books.auth.server.sso

import akka.actor.ActorSystem
import akka.util.Timeout
import com.blinkbox.books.auth.server.SsoConfig
import spray.client.pipelining._
import spray.http.HttpHeaders.Host
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.httpx.unmarshalling.FromResponseUnmarshaller

import scala.concurrent.{ExecutionContext, Future}

sealed trait DecodedResponse { 
  def status: StatusCode
  def headers: List[HttpHeader] 
}
case class DataResponse[T](data: T, status: StatusCode, headers: List[HttpHeader]) extends DecodedResponse
case class UnitResponse(status: StatusCode, headers: List[HttpHeader]) extends DecodedResponse

trait Client {
  def defaultCredentials: HttpCredentials
  def unitRequest(req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[Unit]
  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[T]
  def request[T: FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[DataResponse[T]]
}

trait SprayClient extends Client {
  val config: SsoConfig
  val system: ActorSystem
  val ec: ExecutionContext

  implicit lazy val _timeout = Timeout(config.timeout)
  implicit lazy val _system = system
  implicit lazy val _ec = ec

  protected def doSendReceive: HttpRequest => Future[HttpResponse] = sendReceive

  protected lazy val unitIfSuccessful = { resp: HttpResponse =>
    if (resp.status.isSuccess) () else throw new UnsuccessfulResponseException(resp)
  }

  protected val addHost = { req: HttpRequest =>
    req.withEffectiveUri(securedConnection = true, Host(config.host, config.port))
  }

  protected def basePipeline(credentials: HttpCredentials): SendReceive = {
    addHost ~>
    addCredentials(credentials) ~>
    addHeader("X-CSRF-Protection", "Foobar") ~> // see SSO API doc.
    doSendReceive
  }

  protected def unitPipeline(credentials: HttpCredentials) = basePipeline(credentials) ~> unitIfSuccessful

  protected def dataPipeline[T : FromResponseUnmarshaller](credentials: HttpCredentials) =
    basePipeline(credentials) ~> unmarshal[T]
  
  protected def requestPipeline[T : FromResponseUnmarshaller](credentials: HttpCredentials) = {
    basePipeline(credentials) ~> { resp: HttpResponse =>
      DataResponse(resp ~> unmarshal[T], resp.status, resp.headers)
    }
  }

  override val defaultCredentials = config.credentials

  override def unitRequest(req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[Unit] =
    req ~> unitPipeline(credentials)

  override def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials): Future[T] =
    req ~> dataPipeline[T](credentials)

  override def request[T: FromResponseUnmarshaller](req: HttpRequest, credentials: HttpCredentials = defaultCredentials) =
    req ~> requestPipeline[T](credentials)
}

class DefaultClient(val config: SsoConfig)(implicit val ec: ExecutionContext, val system: ActorSystem) extends SprayClient
