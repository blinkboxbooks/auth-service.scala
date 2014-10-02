package com.blinkbox.books.auth.server.sso

import spray.http.StatusCodes

class TokenStatusSpecs extends SpecBase {

  import env._

  "The SSO client" should "return a successful future if the SSO service replies with a 204" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sso.tokenStatus(SsoRefreshToken("some-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Valid, _, _, Some("refresh"), Some(SsoTokenElevation.Critical), Some(300)) =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in {
    ssoInvalidRequest("Some error")

    failingWith[SsoInvalidRequest](sso.tokenStatus(SsoRefreshToken("some-access-token"))) should matchPattern {
      case SsoInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.tokenStatus(SsoRefreshToken("some-access-token")))
  }

}
