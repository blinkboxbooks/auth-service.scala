package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.{TokenStatusEnv, CommonResponder, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class TokenStatusSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return a successful future if the SSO service replies with a 204" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    whenReady(sso.tokenStatus(SSORefreshToken("some-access-token")))(_ should matchPattern {
      case TokenStatus(SSOTokenStatus.Valid, _, _, "refresh", Some(SSOTokenElevation.Critical), Some(300)) =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in new TestEnv with CommonResponder {
    ssoInvalidRequest("Some error")

    failingWith[SSOInvalidRequest](sso.tokenStatus(SSORefreshToken("some-access-token"))) should matchPattern {
      case SSOInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SSOUnauthorized.type](sso.tokenStatus(SSORefreshToken("some-access-token")))
  }

}
