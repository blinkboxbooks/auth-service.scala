package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.UserInfo
import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.sso._
import org.scalatest.time.{Millis, Span}
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http.{OAuth2BearerToken, StatusCodes}

class UserInfoSpecs extends ApiSpecBase with AuthorisationTestHelpers {

  "The service" should "return user info for an authenticated user that is present on SSO with a critically elevated access token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")
    env.ssoNoContent()
    env.ssoSuccessfulJohnDoeInfo()

    Get("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[UserInfo] should matchPattern {
        case UserInfo(id, _, "john.doe+blinkbox@example.com", "John", "Doe", true) if id == env.userIdA.external =>
      }
    }
  }

  it should "return a 404 if an user id different from the authenticated user is requested" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")

    Get("/users/2") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

  it should "return a 401 unverified identity for an authenticated user that is present on SSO with an unelevated access token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None, tokenType = "access")

    Get("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }

  it should "return a 401 if the authenticated user doesn't have an SSO access token" in {
    Get("/users/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }

  it should "return a 401 if the user is not present on our database but it is available on SSO" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")
    env.ssoNoContent()
    env.ssoSuccessfulJohnDoeInfo()

    val token = env.tokenBuilder.issueAccessToken(
      env.userA.copy(id = UserId(10)), None, env.refreshTokenNoClientA, Some(SsoCredentials(SsoAccessToken("some-access-token"), "bearer", 300, SsoRefreshToken("some-refresh-token"))))

    Get("/users/10") ~> addCredentials(OAuth2BearerToken(token.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }
}
