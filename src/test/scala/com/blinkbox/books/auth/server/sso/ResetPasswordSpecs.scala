package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.TestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class ResetPasswordSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return sso credentials if a valid reset token and a new password are provided" in new TestEnv {
    ssoSuccessfulAuthentication()

    whenReady(sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password"))(_ should matchPattern {
      case cred: SsoUserCredentials =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in new TestEnv {
    ssoInvalidRequest("Some error")

    failingWith[SsoInvalidRequest](sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password")) should matchPattern {
      case SsoInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in new TestEnv {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password"))
  }

}
