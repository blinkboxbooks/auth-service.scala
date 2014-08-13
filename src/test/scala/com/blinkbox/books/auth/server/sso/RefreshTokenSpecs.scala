package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.RefreshTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}

class RefreshTokenSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "complete correctly return sso credentials if the SSO service returns a successful response" in new RefreshTestEnv {
    ssoSuccessfulAuthentication()

    whenReady(sso.refresh("some-refresh-token")) { creds =>
      creds should matchPattern {
        case SSOCredentials(_, "bearer", exp, _) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "correctly handle a bad-request response from the SSO service" in new RefreshTestEnv {
    val err = "The request is invalid"
    ssoInvalidRequest(err)

    failingWith[SSOInvalidRequest](sso.refresh("some-refresh-token")) should matchPattern {
      case SSOInvalidRequest(m) if m == err =>
    }
  }
}
