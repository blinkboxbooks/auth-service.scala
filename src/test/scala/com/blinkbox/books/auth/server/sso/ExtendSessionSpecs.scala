package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.{TestEnv, CommonResponder}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.{StatusCodes, StatusCode}

class ExtendSessionSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "extend a given SSO session" in new TestEnv with CommonResponder {
    ssoNoContent()

    whenReady(sso.extendSession(SsoAccessToken("some-access-token"))) { _ => }
  }

  it should "return an Unauthorized exception if the SSO service signals invalid credentials" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.extendSession(SsoAccessToken("some-access-token"))) should equal(SsoUnauthorized)
  }
}
