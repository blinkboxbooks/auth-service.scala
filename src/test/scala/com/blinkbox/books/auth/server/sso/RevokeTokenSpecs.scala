package com.blinkbox.books.auth.server.sso

import spray.http.StatusCodes

class RevokeTokenSpecs extends SpecBase {

  import env._

  "The SSO client" should "return a successful future if the SSO service replies with a 204" in {
    ssoNoContent()

    whenReady(sso.revokeToken(SsoRefreshToken("some-refresh-token"))) { _ => }
  }

  it should "signal a request failure if the SSO service replies with a 400" in {
    ssoInvalidRequest("Some error")

    failingWith[SsoInvalidRequest](sso.revokeToken(SsoRefreshToken("some-refresh-token"))) should matchPattern {
      case SsoInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.revokeToken(SsoRefreshToken("some-refresh-token")))
  }

}
