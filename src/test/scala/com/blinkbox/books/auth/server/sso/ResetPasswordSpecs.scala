package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.{AuthenticationTestEnv, CommonResponder, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class ResetPasswordSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return sso credentials if a valid reset token and a new password are provided" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()

    whenReady(sso.resetPassword(SSOPasswordResetToken("some-token"), "new-password"))(_ should matchPattern {
      case cred: SSOUserCredentials =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in new TestEnv with CommonResponder {
    ssoInvalidRequest("Some error")

    failingWith[SSOInvalidRequest](sso.resetPassword(SSOPasswordResetToken("some-token"), "new-password")) should matchPattern {
      case SSOInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SSOUnauthorized.type](sso.resetPassword(SSOPasswordResetToken("some-token"), "new-password"))
  }

}
