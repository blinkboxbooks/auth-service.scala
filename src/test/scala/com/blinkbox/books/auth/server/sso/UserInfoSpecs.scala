package com.blinkbox.books.auth.server.sso

class UserInfoSpecs extends SpecBase {

  import env._

  "The SSO client" should "retrieve user information if the SSO service responds with a success" in {
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
