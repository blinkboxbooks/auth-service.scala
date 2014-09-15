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

  it should "correctly serialize non-ASCII characters" in {
    ssoSuccessfulRegistration()

    whenReady(sso.register(reg.copy(firstName = "Iñtërnâtiônàlizætiøn", lastName = "中国扬声器可以阅读本"))) { cred =>
      cred should matchPattern {
        case (_, SsoCredentials(_, "bearer", exp, _)) if exp == validTokenSSOExpiry =>
      }

      ssoRequests shouldNot be(empty)
      ssoRequests.last.entity.asString should equal(
        "first_name=I%C3%B1t%C3%ABrn%C3%A2ti%C3%B4n%C3%A0liz%C3%A6ti%C3%B8n&username=anusername@test.tst&" +
          "last_name=%E4%B8%AD%E5%9B%BD%E6%89%AC%E5%A3%B0%E5%99%A8%E5%8F%AF%E4%BB%A5%E9%98%85%E8%AF%BB%E6%9C%AC&" +
          "grant_type=urn:blinkbox:oauth:grant-type:registration&password=a-password")
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
