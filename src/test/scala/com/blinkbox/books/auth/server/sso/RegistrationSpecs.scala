package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.UserRegistration

class RegistrationSpecs extends SpecBase {

  import env._

  val reg = UserRegistration("A name", "A surname", "anusername@test.tst", "a-password", true, true, None, None, None, None)

  "The SSO client" should "return token credentials for a valid response from the SSO service" in {
    ssoSuccessfulRegistration()

    whenReady(sso.register(reg)) { cred =>
      cred should matchPattern {
        case (_, SsoCredentials(_, "bearer", exp, _)) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "signal a conflict if the SSO service signals that the username is already taken" in {
    ssoConflict()

    failingWith[SsoConflict.type](sso.register(reg))
  }

  it should "signal an invalid request if the SSO service signals validation errors" in {
    val err = "Password does not meet minimum requirements"
    ssoInvalidRequest(err)

    failingWith[SsoInvalidRequest](sso.register(reg)) should matchPattern {
      case SsoInvalidRequest(m) if m == err =>
    }
  }
}
