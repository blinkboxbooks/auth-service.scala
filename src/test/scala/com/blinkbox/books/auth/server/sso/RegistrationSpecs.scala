package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.UserRegistration
import com.blinkbox.books.auth.server.env.RegistrationTestEnv
import org.scalatest.{FlatSpec, Matchers}
import spray.http._

class RegistrationSpecs extends FlatSpec with Matchers with SpecBase {
  "The SSO client" should "return token credentials for a valid response" in new RegistrationTestEnv {
    ssoSuccessfulRegistration()

    whenReady(sso.register(UserRegistration("A name", "A surname", "anusername@test.tst", "a-password", true, true, None, None, None, None))) { cred =>
      cred should matchPattern {
        case SSOCredentials(_, "bearer", _, _) =>
      }
    }
  }
}
