package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorRef, ActorSystem}
import com.blinkbox.books.auth.server.SSOConfig
import org.scalatest.Matchers._
import spray.http.{MediaTypes, HttpEntity, HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContext, Future, Promise}

class TestSSOClient(
    config: SSOConfig,
    nextResponse: () => Future[HttpResponse])(implicit ec: ExecutionContext, sys: ActorSystem) extends DefaultClient(config) {

  private val commonAssertions: HttpRequest => Unit = { req =>
    req.headers.find(_.name.toLowerCase == "x-csrf-protection") shouldBe defined

    req.entity match {
      case HttpEntity.NonEmpty(ct, _) => ct.mediaType should equal(MediaTypes.`application/x-www-form-urlencoded`)
      case _ => // Nothing to check if the entity is empty
    }
  }

  override def doSendReceive(transport: ActorRef): HttpRequest => Future[HttpResponse] = { req: HttpRequest =>
    commonAssertions(req)
    nextResponse()
  }
}

class SSOResponseMocker {
  private var ssoResponse = List.empty[Promise[HttpResponse]]

  def complete(completions: (Promise[HttpResponse] => Unit)*): Unit = {
    for (c <- completions) {
      val p = Promise[HttpResponse]
      c(p)
      ssoResponse = p :: ssoResponse
    }
  }

  def nextResponse(): Future[HttpResponse] = ssoResponse.reverse match {
    case p :: ps => p.future
    case _ => sys.error("Expected SSO response mock, got nothing")
  }
}
