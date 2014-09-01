package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.PasswordCredentials
import com.blinkbox.books.auth.server.env.AuthenticationTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}

class AuthenticationSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  val credentials = PasswordCredentials("foo", "bar", None, None)

  "The SSO client" should "return token credentials for a valid response from the SSO service" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()

    whenReady(sso.authenticate(credentials)) { ssoCreds =>
      ssoCreds should matchPattern {
        case SsoCredentials(_, "bearer", exp, _) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "return an invalid request response if the SSO service returns a bad-request response" in new AuthenticationTestEnv {
    val err = "Invalid username or password"
    ssoInvalidRequest(err)

    failingWith[SsoInvalidRequest](sso.authenticate(credentials)) should matchPattern {
      case SsoInvalidRequest(m) if m == err =>
    }
  }

  it should "return an authentication error if the SSO service doesn't recognize given credentials" in new AuthenticationTestEnv {
    ssoUnsuccessfulAuthentication()

    failingWith[SsoUnauthorized.type](sso.authenticate(credentials))
  }

  it should "correctly signal when password throttling errors are returned from the SSO service" in new AuthenticationTestEnv {
    ssoTooManyRequests(10)

    failingWith[SsoTooManyRequests](sso.authenticate(credentials)) should matchPattern {
      case SsoTooManyRequests(d) if d.toSeconds == 10 =>
    }
  }
}
