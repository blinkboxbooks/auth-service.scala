package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.sso.{SsoRefreshToken, SsoCredentials, SsoAccessToken}
import com.blinkbox.books.auth.server.{TokenBuilder, UserInfo}
import com.blinkbox.books.auth.server.env.UserInfoTestEnv
import spray.http.{StatusCodes, OAuth2BearerToken}

class UserInfoSpecs extends ApiSpecBase[UserInfoTestEnv] {

  override def newEnv = new UserInfoTestEnv

  "The service" should "return user info for an authenticated user that is present on SSO" in {
    env.ssoSuccessfulJohnDoeInfo()

    Get("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[UserInfo] should matchPattern {
        case UserInfo(id, _, "john.doe+blinkbox@example.com", "John", "Doe", true) if id == env.userIdA.external =>
      }
    }
  }

  it should "return a 404 if an user id different from the authenticated user is requested" in {
    env.ssoNoInvocation()

    Get("/users/2") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

  it should "return a 401 if the authenticated user doesn't have an SSO access token" in {
    env.ssoNoInvocation()

    Get("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
    }
  }

  it should "return a 401 if the user is not present on our database but it is available on SSO" in {
    env.ssoSuccessfulJohnDoeInfo()

    val token = env.tokenBuilder.issueAccessToken(
      env.userA.copy(id = UserId(10)), None, env.refreshTokenNoClientA, Some(SsoCredentials(SsoAccessToken("some-access-token"), "bearer", 300, SsoRefreshToken("some-refresh-token"))))

    Get("/users/10") ~> addCredentials(OAuth2BearerToken(token.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
    }
  }
}
