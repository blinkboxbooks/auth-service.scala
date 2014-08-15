package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.env.TokenStatusEnv
import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import com.blinkbox.books.auth.server.{RefreshTokenStatus, SessionInfo}
import spray.http.{OAuth2BearerToken, StatusCodes}

class SessionSpecs extends ApiSpecBase[TokenStatusEnv] {

  override def newEnv = new TokenStatusEnv

  "The service" should "return session information for a valid and critically elevated token" in {
    cancel("This test needs fixing on the authenticator")
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
      }
    }
  }

  it should "return session information for a valid non elevated token" in {
    cancel("This test needs fixing on the authenticator")
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None)

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
      }
    }
  }

  it should "return session information for a valid token that doesn't have a corresponding SSO token" in {
    cancel("This test needs fixing on the authenticator")
    env.ssoNoInvocation()
    env.removeSSOTokens()

    Get("/session") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      jsonResponseAs[SessionInfo] should matchPattern {
        case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
      }
    }
  }
}
