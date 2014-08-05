package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.UserRegistration
import org.scalatest.{FlatSpec, Matchers}
import spray.http._

class RegistrationSpecs extends FlatSpec with Matchers with SpecBase {
  "The SSO client" should "return token credentials for a valid response" in new SSOTestEnv {
    val json = """{
      "access_token":"2YotnFZFEjr1zCsicMWpAA",
      "token_type":"bearer",
      "expires_in":600,
      "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
    }"""

    ssoResponse.complete(_.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, json.getBytes))))

    whenReady(sso.register(UserRegistration("A name", "A surname", "anusername@test.tst", "a-password", true, true, None, None, None, None))) { cred =>
      cred should matchPattern {
        case TokenCredentials("2YotnFZFEjr1zCsicMWpAA", "bearer", 600, "tGzv3JOkF0XG5Qx2TlKWIA") =>
      }
    }
  }
}
