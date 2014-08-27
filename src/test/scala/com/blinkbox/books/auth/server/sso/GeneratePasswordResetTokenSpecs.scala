package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.PasswordResetEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}

class GeneratePasswordResetTokenSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return token credentials for a valid response from the SSO service" in new PasswordResetEnv {
    ssoGenerateResetToken

    whenReady(sso.generatePasswordResetToken("foo@bar.com"))(_ should matchPattern {
      case SSOPasswordResetToken("r3sett0ken", 3600) =>
    })
  }

  it should "return an error if the user is not found in the SSO service" in new PasswordResetEnv {
    ssoUserNotFound

    failingWith[SSONotFound.type](sso.generatePasswordResetToken("foo@bar.com")) should equal(SSONotFound)
  }

}
