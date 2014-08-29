package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.env.TokenStatusEnv
import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import com.blinkbox.books.auth.server.{TokenStatus, SessionInfo}
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http.{HttpEntity, OAuth2BearerToken, StatusCodes}

class SessionSpecs extends ApiSpecBase[TokenStatusEnv] {

  override def newEnv = new TokenStatusEnv

  "The service" should "return session information for a valid and critically elevated token" in {
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
      }
    }
  }

  it should "return session information for a valid non elevated token" in {
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None)

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
      }
    }
  }

  it should "return session information for a valid token that doesn't have a corresponding SSO token" in {
    env.ssoNoInvocation()

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
      }
    }
  }

  it should "extend an user session by invoking the SSO service" in {
    env.ssoNoContent()
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
      }
    }
  }

  it should "not extend an user session if the authenticated user doesn't have an SSO token" in {
    env.ssoNoInvocation()

    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
      header[`WWW-Authenticate`] shouldBe defined
    }
  }

  it should "signal that the SSO service didn't recognize the provided credentials when extending a session" in {
    env.ssoResponse(StatusCodes.Unauthorized)

    Post("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
      header[`WWW-Authenticate`] shouldBe defined
    }
  }
}
