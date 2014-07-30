package com.blinkbox.books.auth.server.sso

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import spray.httpx.UnsuccessfulResponseException

trait Client {
  def unitRequest(req: HttpRequest): Future[Unit]
  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest): Future[T]
}

class DefaultClient(config: SSOConfig)(implicit val ec: ExecutionContext, system: ActorSystem) extends Client {
  private implicit val setupTimeout = Timeout(500.millis)

  private val unitIfSuccessful = { resp: HttpResponse =>
    if (resp.status.isSuccess) () else throw new UnsuccessfulResponseException(resp)
  }

  private val basePipeline: Future[SendReceive] = for {
    Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(config.host, port = config.port)
  } yield {
    addCredentials(config.credentials) ~>
    addHeader("Content-Type", "application/x-www-form-urlencoded") ~>
    addHeader("X-CSRF-Protection", "Foobar") ~> // This is needed for some obscure reason (see SSO API doc.)
    sendReceive(connector)
  }

  private val unitPipeline = basePipeline map { _ ~> unitIfSuccessful }

  private def dataPipeline[T : FromResponseUnmarshaller] = basePipeline map { _ ~> unmarshal[T] }

  def unitRequest(req: HttpRequest): Future[Unit] = unitPipeline.flatMap(_(req))

  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest): Future[T] = dataPipeline[T].flatMap(_(req))
}
