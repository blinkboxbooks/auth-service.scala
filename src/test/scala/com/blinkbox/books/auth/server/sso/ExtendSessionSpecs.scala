package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.TestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.{StatusCodes, StatusCode}

class ExtendSessionSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "extend a given SSO session" in new TestEnv {
    ssoNoContent()

    whenReady(sso.extendSession(SsoAccessToken("some-access-token"))) { _ => }
  }

  it should "return an Unauthorized exception if the SSO service signals invalid credentials" in new TestEnv {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.extendSession(SsoAccessToken("some-access-token"))) should equal(SsoUnauthorized)
  }
}
