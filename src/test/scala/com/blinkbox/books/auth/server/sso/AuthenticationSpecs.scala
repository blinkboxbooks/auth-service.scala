package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.PasswordCredentials

class AuthenticationSpecs extends SpecBase {

  import env._

  val credentials = PasswordCredentials("foo", "bar", None, None)

  "The SSO client" should "return token credentials for a valid response from the SSO service" in {
    ssoSuccessfulAuthentication()

    whenReady(sso.authenticate(credentials)) { ssoCreds =>
      ssoCreds should matchPattern {
        case SsoAuthenticatedCredentials(_, SsoCredentials(_, "bearer", exp, _), MigrationStatus.NoMigration) if exp == validTokenSSOExpiry =>
      }
    }
  }

  it should "return the correct migration status in case an user migrated with a partial-match to SSO while authenticating" in {
    ssoSuccessfulAuthentication(MigrationStatus.PartialMatch)

    whenReady(sso.authenticate(credentials)) { ssoCreds =>
      ssoCreds.migrationStatus should equal(MigrationStatus.PartialMatch)
    }
  }

  it should "return the correct migration status in case an user migrated with a total-match to SSO while authenticating" in {
    ssoSuccessfulAuthentication(MigrationStatus.TotalMatch)

    whenReady(sso.authenticate(credentials)) { ssoCreds =>
      ssoCreds.migrationStatus should equal(MigrationStatus.TotalMatch)
    }
  }

  it should "return an invalid request response if the SSO service returns a bad-request response" in {
    val err = "Invalid username or password"
    ssoInvalidRequest(err)

    failingWith[SsoInvalidRequest](sso.authenticate(credentials)) should matchPattern {
      case SsoInvalidRequest(m) if m == err =>
    }
  }

  it should "return an authentication error if the SSO service doesn't recognize given credentials" in {
    ssoUnsuccessfulAuthentication()

    failingWith[SsoUnauthorized.type](sso.authenticate(credentials))
  }

  it should "correctly signal when password throttling errors are returned from the SSO service" in {
    ssoTooManyRequests(10)

    failingWith[SsoTooManyRequests](sso.authenticate(credentials)) should matchPattern {
      case SsoTooManyRequests(d) if d.toSeconds == 10 =>
    }
  }
}
