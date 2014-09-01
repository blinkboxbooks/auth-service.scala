package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.UserInfoTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}

class UserInfoSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "retrieve user information if the SSO service responds with a success" in new UserInfoTestEnv {
    ssoSuccessfulJohnDoeInfo()

    whenReady(sso.userInfo(SsoAccessToken("some-access-token"))) { userInfo =>
      userInfo should matchPattern {
        case UserInformation(
          SsoUserId("6E41CB9F"),
          "john.doe+blinkbox@example.com",
          "John",
          "Doe",
          LinkedAccount(
            "music",
            "john.doe@music.com",
            true) :: Nil) =>
      }
    }
  }
}
