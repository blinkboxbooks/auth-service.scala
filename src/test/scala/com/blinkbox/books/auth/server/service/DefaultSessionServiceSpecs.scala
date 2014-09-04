package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulAuthorizationErrorCode.InvalidToken
import com.blinkbox.books.auth.server.ZuulAuthorizationErrorReason.UnverifiedIdentity
import com.blinkbox.books.auth.server.env.TestEnv
import com.blinkbox.books.auth.server.sso.{SsoTokenElevation, SsoTokenStatus}
import com.blinkbox.books.auth.server.{TokenStatus, SessionInfo, ZuulAuthorizationException}
import com.blinkbox.books.auth.{Elevation, User}
import spray.http.StatusCodes

class DefaultSessionServiceSpecs extends SpecBase {

  implicit val user = User(Map(
    "sub" -> "urn:blinkbox:zuul:user:1",
    "zl/rti" -> Int.box(1),
    "sso/at" -> "some-access-token"
  ))

  "The session service" should "report the status of an access token bound to a critically elevated SSO token" in new TestEnv {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
    })
  }

  it should "report the status of an access token bound to an un-elevated SSO token" in new TestEnv {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

  it should "report the status of an access token bound to a revoked SSO token" in new TestEnv {
    ssoSessionInfo(SsoTokenStatus.Revoked, SsoTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of an access token bound to an expired SSO token" in new TestEnv {
    ssoSessionInfo(SsoTokenStatus.Expired, SsoTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of an access token bound to an invalid SSO token" in new TestEnv {
    ssoSessionInfo(SsoTokenStatus.Invalid, SsoTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "use zuul information to provide status of an access token that is not bound to any SSO token" in new TestEnv {
    ssoNoInvocation()

    val u = user.copy(claims = user.claims - "sso/at")

    whenReady(sessionService.querySession()(u))(_ should matchPattern {
      case SessionInfo(TokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

  it should "extend an user session by invoking the SSO service" in new TestEnv {
    ssoNoContent()
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sessionService.extendSession())(_ should matchPattern {
      case SessionInfo(TokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
    })
  }

  it should "not extend an user session if the authenticated user doesn't have an SSO token" in new TestEnv {
    ssoNoInvocation()

    val u = user.copy(claims = user.claims - "sso/at")

    failingWith[ZuulAuthorizationException](sessionService.extendSession()(u)) should matchPattern {
      case ZuulAuthorizationException(_, InvalidToken, Some(UnverifiedIdentity)) =>
    }
  }

  it should "signal that the SSO service didn't recognize the provided credentials when extending a session" in new TestEnv {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[ZuulAuthorizationException](sessionService.extendSession()) should matchPattern {
      case ZuulAuthorizationException(_, InvalidToken, Some(UnverifiedIdentity)) =>
    }
  }
}
