package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorSystem, ActorRef}
import com.blinkbox.books.auth.server.SSOConfig
import org.scalatest.Matchers._
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.{Promise, ExecutionContext, Future}

class TestSSOClient(
    config: SSOConfig,
    nextResponse: () => Future[HttpResponse])(implicit ec: ExecutionContext, sys: ActorSystem) extends DefaultClient(config) {

  private val commonAssertions: HttpRequest => Unit = { req =>
    req.headers.find(_.name.toLowerCase == "x-csrf-protection") shouldBe defined

    val contentType = req.headers.find(_.name.toLowerCase == "content-type")
    contentType shouldBe defined
    contentType foreach { _.value should equal("application/x-www-form-urlencoded") }
  }

  override def doSendReceive(transport: ActorRef): HttpRequest => Future[HttpResponse] = { req: HttpRequest =>
    commonAssertions(req)
    nextResponse()
  }
}

class SSOResponseMocker {
  private var ssoResponse = List.empty[Promise[HttpResponse]]

  def complete(completion: Promise[HttpResponse] => Unit): Unit = {
    val p = Promise[HttpResponse]
    completion(p)
    ssoResponse = p :: ssoResponse
  }

  def nextResponse(): Future[HttpResponse] = ssoResponse.reverse match {
    case p :: ps => p.future
    case _ => sys.error("Expected SSO response mock, got nothing")
  }
}
