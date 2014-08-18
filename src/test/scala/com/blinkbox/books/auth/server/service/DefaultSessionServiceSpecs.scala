package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.auth.server.{RefreshTokenStatus, SessionInfo}
import com.blinkbox.books.auth.server.env.TokenStatusEnv

class DefaultSessionServiceSpecs extends SpecBase {

  implicit val user = User(Map[String, AnyRef](
    "sub" -> "urn:blinkbox:zuul:user:1",
    "zl/rti" -> "1"
  ))

  "The session service" should "report the status of a refresh token bound to a critically elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
    })
  }

  it should "report the status of a refresh token bound to an un-elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

  it should "report the status of a refresh token bound to a revoked SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Revoked, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of a refresh token bound to an expired SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Expired, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of a refresh token bound to an invalid SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Invalid, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "use zuul information to provide status of a refresh token that is not bound to any SSO token" in new TokenStatusEnv {
    ssoNoInvocation()
    removeSSOTokens()

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

}
