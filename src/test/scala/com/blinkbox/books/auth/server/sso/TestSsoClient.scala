package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorRef, ActorSystem}
import com.blinkbox.books.auth.server.SsoConfig
import org.scalatest.Matchers._
import spray.http.{HttpEntity, HttpRequest, HttpResponse, MediaTypes}

import scala.concurrent.{ExecutionContext, Future, Promise}

class TestSsoClient(
    config: SsoConfig,
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

class SsoResponseMocker {
  private var ssoResponse = List.empty[Promise[HttpResponse]]

  def complete(completions: (Promise[HttpResponse] => Unit)*): Unit = {
    for (c <- completions) {
      val p = Promise[HttpResponse]
      c(p)
      ssoResponse = ssoResponse :+ p
    }
  }

  def nextResponse(): Future[HttpResponse] = ssoResponse match {
    case p :: ps =>
      ssoResponse = ps
      p.future
    case _ => sys.error("Expected SSO response mock, got nothing")
  }

  def reset(): Unit = { ssoResponse = Nil }
}
