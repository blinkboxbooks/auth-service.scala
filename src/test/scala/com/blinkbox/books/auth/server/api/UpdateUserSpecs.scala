package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.UserInfo
import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.sso._
import spray.http.{FormData, OAuth2BearerToken, StatusCodes}

class UpdateUserSpecs extends ApiSpecBase {

  val userPatch = FormData(Map(
    "first_name" -> "Foo",
    "last_name" -> "Bar"
  ))

  "The service" should "update user info for an authenticated user that is present on SSO with a critically elevated access token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")
    env.ssoSuccessfulJohnDoeInfo()
    env.ssoNoContent()

    Patch("/users/1", userPatch) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[UserInfo] should matchPattern {
        case UserInfo(id, _, "john.doe+blinkbox@example.com", "Foo", "Bar", true) if id == env.userIdA.external =>
      }
    }
  }

  it should "return a 404 if an user id different from the authenticated user is requested" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")

    Patch("/users/2", userPatch) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

  it should "return a 401 unverified identity for an authenticated user that is present on SSO with an unelevated access token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None, tokenType = "access")

    Patch("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }

  it should "return a 401 if the authenticated user doesn't have an SSO access token" in {
    Patch("/users/1", userPatch) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }

  it should "return a 401 if the user is not present on our database but it is available on SSO" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")
    env.ssoSuccessfulJohnDoeInfo()

    val token = env.tokenBuilder.issueAccessToken(
      env.userA.copy(id = UserId(10)), None, env.refreshTokenNoClientA, Some(SsoCredentials(SsoAccessToken("some-access-token"), "bearer", 300, SsoRefreshToken("some-refresh-token"))))

    Patch("/users/10", userPatch) ~> addCredentials(OAuth2BearerToken(token.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }
}
