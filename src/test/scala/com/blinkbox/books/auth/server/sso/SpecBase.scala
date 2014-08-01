package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers
import org.scalatest.time._
import scala.concurrent.Future
import scala.concurrent.duration._
import spray.http._

trait SpecBase extends ScalaFutures with Matchers {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  val ssoConfig = SSOConfig("test.tst", 9999, "v1", BasicHttpCredentials("foo", "bar"), 500.millis)

  type HttpRequestAssertions = HttpRequest => Unit
  val nullAssertions: HttpRequestAssertions = _ => ()
  val commonAssertions: HttpRequestAssertions = { req =>
    req.headers.find(_.name.toLowerCase == "x-csrf-protection") shouldBe defined

    val contentType = req.headers.find(_.name.toLowerCase == "content-type")
    contentType shouldBe defined
    contentType foreach { _.value should equal("application/x-www-form-urlencoded") }
  }

  def mockClient(resp: HttpResponse, reqAsserts: HttpRequestAssertions) = new SprayClient {
    val system = ActorSystem()
    val ec = system.dispatcher
    val timeout = Timeout(500.millis)
    val config = ssoConfig

    override def doSendReceive(transport: ActorRef): HttpRequest => Future[HttpResponse] = { req: HttpRequest =>
      commonAssertions(req)
      reqAsserts(req)
      Future.successful(resp)
    }
  }

  def mockExecutors(resp: HttpResponse, reqAsserts: HttpRequestAssertions) = new SSOExecutors(ssoConfig, mockClient(resp, reqAsserts))

  def withResponse[A](resp: HttpResponse)(req: SSOExecutors => Future[A], reqAsserts: HttpRequestAssertions = nullAssertions): Future[A] = {
    val executors = mockExecutors(resp, reqAsserts)
    req(executors)
  }

  lazy val sso = new DefaultSSO
}
