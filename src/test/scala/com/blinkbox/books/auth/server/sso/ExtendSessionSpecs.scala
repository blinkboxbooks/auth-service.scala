package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.CommonResponder
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.{StatusCodes, StatusCode}

class ExtendSessionSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "extend a given SSO session" in new SSOTestEnv with CommonResponder {
    ssoNoContent()

    whenReady(sso.extendSession(SSOAccessToken("some-access-token"), "a-refresh-token")) { _ => }
  }

  it should "return an Unauthorized exception if the SSO service signals invalid credentials" in new SSOTestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SSOUnauthorized.type](sso.extendSession(SSOAccessToken("some-access-token"), "a-refresh-token")) should equal(SSOUnauthorized)
  }
}
