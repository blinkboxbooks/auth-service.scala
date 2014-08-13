package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.UserRegistration
import com.blinkbox.books.auth.server.env.RegistrationTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http._

class RegistrationSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {
  val reg = UserRegistration("A name", "A surname", "anusername@test.tst", "a-password", true, true, None, None, None, None)

  "The SSO client" should "return token credentials for a valid response from the SSO service" in new RegistrationTestEnv {
    ssoSuccessfulRegistration()

    whenReady(sso.register(reg)) { cred =>
      cred should matchPattern {
        case (_, SSOCredentials(_, "bearer", exp, _)) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "signal a conflict if the SSO service signals that the username is already taken" in new RegistrationTestEnv {
    ssoConflict()

    failingWith[SSOConflict.type](sso.register(reg))
  }

  it should "signal an invalid request if the SSO service signals validation errors" in new RegistrationTestEnv {
    val err = "Password does not meet minimum requirements"
    ssoInvalidRequest(err)

    failingWith[SSOInvalidRequest](sso.register(reg)) should matchPattern {
      case SSOInvalidRequest(m) if m == err =>
    }
  }
}
