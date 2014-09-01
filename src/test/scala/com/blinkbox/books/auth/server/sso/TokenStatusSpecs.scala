package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.{TokenStatusEnv, CommonResponder, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class TokenStatusSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return a successful future if the SSO service replies with a 204" in new TokenStatusEnv {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sso.tokenStatus(SsoRefreshToken("some-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Valid, _, _, "refresh", Some(SsoTokenElevation.Critical), Some(300)) =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in new TestEnv with CommonResponder {
    ssoInvalidRequest("Some error")

    failingWith[SsoInvalidRequest](sso.tokenStatus(SsoRefreshToken("some-access-token"))) should matchPattern {
      case SsoInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.tokenStatus(SsoRefreshToken("some-access-token")))
  }

}
