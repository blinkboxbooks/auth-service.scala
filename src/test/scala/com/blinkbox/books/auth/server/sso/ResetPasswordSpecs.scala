package com.blinkbox.books.auth.server.sso

import spray.http.StatusCodes

class ResetPasswordSpecs extends SpecBase {

  import env._

  "The SSO client" should "return sso credentials if a valid reset token and a new password are provided" in {
    ssoSuccessfulAuthentication()

    whenReady(sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password"))(_ should matchPattern {
      case cred: SsoUserCredentials =>
    })
  }

  it should "signal a request failure if the SSO service replies with a 400" in {
    ssoInvalidRequest("Some error")

    failingWith[SsoInvalidRequest](sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password")) should matchPattern {
      case SsoInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SsoUnauthorized.type](sso.resetPassword(SsoPasswordResetToken("some-token"), "new-password"))
  }

}
