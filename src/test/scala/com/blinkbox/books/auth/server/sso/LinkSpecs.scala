package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId

class LinkSpecs extends SpecBase {

  import env._

  val accessToken = SsoAccessToken("some-access-token")
  val userId = UserId(123)

  "The SSO client" should "complete correctly a link request if the SSO service respond with a success" in {
    ssoNoContent()

    whenReady(sso.linkAccount(accessToken, userId, true, "1.0")) { _ => }
  }

  it should "correctly convert a bad-request response from the SSO service" in {
    val err = "The request is not valid"
    ssoInvalidRequest(err)

    failingWith[SsoInvalidRequest](sso.linkAccount(accessToken, userId, true, "1.0")) should matchPattern {
      case SsoInvalidRequest(m) if m == err =>
    }
  }

  it should "correctly handle conflict responses if the account is already linked" in {
    ssoConflict()

    failingWith[SsoConflict.type](sso.linkAccount(accessToken, userId, true, "1.0"))
  }
}
