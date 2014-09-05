package com.blinkbox.books.auth.server.sso

import spray.http.StatusCodes

class ExtendSessionSpecs extends SpecBase {

  import env._

  "The SSO client" should "extend a given SSO session" in {
    ssoNoContent()

    whenReady(sso.extendSession(SsoAccessToken("some-access-token"))) { _ => }
  }

  it should "return an Unauthorized exception if the SSO service signals invalid credentials" in {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.extendSession(SsoAccessToken("some-access-token"))) should equal(SsoUnauthorized)
  }
}
