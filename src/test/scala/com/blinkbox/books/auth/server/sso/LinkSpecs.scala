package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.env.LinkTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}

class LinkSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  val ssoCredentials = SSOCredentials("some-access-token", "bearer", 600, "some-refresh-token")
  val userId = UserId(123)

  "The SSO client" should "complete correctly a link request if the SSO service respond with a success" in new LinkTestEnv {
    ssoNoContent()

    whenReady(sso.linkAccount(ssoCredentials, userId, true, "1.0")) { _ => }
  }

  it should "correctly convert a bad-request response from the SSO service" in new LinkTestEnv {
    val err = "The request is not valid"
    ssoInvalidRequest(err)

    failingWith[SSOInvalidRequest](sso.linkAccount(ssoCredentials, userId, true, "1.0")) should matchPattern {
      case SSOInvalidRequest(m) if m == err =>
    }
  }

  it should "correctly handle conflict responses if the account is already linked" in new LinkTestEnv {
    ssoConflict()

    failingWith[SSOConflict.type](sso.linkAccount(ssoCredentials, userId, true, "1.0"))
  }
}
