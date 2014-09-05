package com.blinkbox.books.auth.server.sso

class GeneratePasswordResetTokenSpecs extends SpecBase {
  import env._

  "The SSO client" should "return token credentials for a valid response from the SSO service" in {
    ssoGenerateResetToken

    whenReady(sso.generatePasswordResetToken("foo@bar.com"))(_ should matchPattern {
      case SsoPasswordResetTokenResponse(SsoPasswordResetToken("r3sett0ken"), 3600) =>
    })
  }

  it should "return an error if the user is not found in the SSO service" in {
    ssoUserNotFound

    failingWith[SsoNotFound.type](sso.generatePasswordResetToken("foo@bar.com")) should equal(SsoNotFound)
  }

}
