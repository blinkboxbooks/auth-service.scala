package com.blinkbox.books.auth.server.sso

class RefreshTokenSpecs extends SpecBase {

  import env._

  "The SSO client" should "complete correctly return sso credentials if the SSO service returns a successful response" in {
    ssoSuccessfulAuthentication()

    whenReady(sso.refresh(SsoRefreshToken("some-refresh-token"))) { creds =>
      creds should matchPattern {
        case SsoCredentials(_, "bearer", exp, _) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "correctly handle a bad-request response from the SSO service" in {
    val err = "The request is invalid"
    ssoInvalidRequest(err)

    failingWith[SsoInvalidRequest](sso.refresh(SsoRefreshToken("some-refresh-token"))) should matchPattern {
      case SsoInvalidRequest(m) if m == err =>
    }
  }
}
