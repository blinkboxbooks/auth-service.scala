package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.{UserRole, Elevation}
import com.blinkbox.books.auth.server.sso.{SsoTokenElevation, SsoTokenStatus}
import com.blinkbox.books.auth.server.{SessionInfo, TokenStatus}
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http.{OAuth2BearerToken, StatusCodes}

class SessionSpecs extends ApiSpecBase {

  "The service" should "return session information for a valid and critically elevated token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), Some(Nil)) =>
      }
    }
  }

  it should "return session information for a valid non elevated token" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None, tokenType = "access")

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, Some(Nil)) =>
      }
    }
  }

  it should "return session information including roles for a token that has associated roles" in {
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None, tokenType = "access")

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoC.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, Some(roles))
          if roles.toSet == Set(UserRole.ContentManager.toString, UserRole.Employee.toString) =>
      }
    }
  }

  it should "return session information for a valid token that doesn't have a corresponding SSO token" in {
    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, Some(Nil)) =>
      }
    }
  }

  it should "extend an user session by invoking the SSO service" in {
    env.ssoNoContent()
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), Some(Nil)) =>
      }
    }
  }

  it should "not extend an user session if the authenticated user doesn't have an SSO token" in {
    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }

  it should "signal that the SSO service didn't recognize the provided credentials when extending a session" in {
    env.ssoResponse(StatusCodes.Unauthorized)

    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      assertUnauthorisedWithUnverifiedIdentity()
    }
  }
}
