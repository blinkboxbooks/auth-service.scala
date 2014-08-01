package com.blinkbox.books.auth.server.sso

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.blinkbox.books.auth.server.data.UserId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.http._
import spray.httpx.unmarshalling.FromResponseUnmarshaller

class RegistrationSpecs extends FlatSpec with Matchers with SpecBase {
  "The SSO client" should "return token credentials for a valid response" in {
    val json = """{
      "access_token":"2YotnFZFEjr1zCsicMWpAA",
      "token_type":"bearer",
      "expires_in":600,
      "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
    }"""

    val resp = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, json.getBytes))

    val f = withResponse(resp)({ ex =>
      import ex._
      sso.perform(RegisterUser(UserId(123), "A name", "A surname", "anusername@test.tst", "a-password", "1.0", true))
    })

    whenReady(f) { cred =>
      cred should matchPattern {
        case TokenCredentials("2YotnFZFEjr1zCsicMWpAA", "bearer", 600, "tGzv3JOkF0XG5Qx2TlKWIA") =>
      }
    }
  }
}
