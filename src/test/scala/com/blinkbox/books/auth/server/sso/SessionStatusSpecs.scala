package com.blinkbox.books.auth.server.sso

import org.joda.time.{DateTime, DateTimeZone}

class SessionStatusSpecs extends SpecBase {

  import env._

  def checkDates(issued: DateTime, expiry: DateTime) =
    issued == new DateTime("2000-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC) &&
    expiry == new DateTime("2020-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC)

  "The SSO client" should "return session info for a valid, critically elevated SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sso.sessionStatus(SsoAccessToken("an-acces-token")))(_ should matchPattern {
      case SessionStatus(SsoTokenStatus.Valid, issued, expiry, "refresh", SsoTokenElevation.Critical, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a valid non-elevated SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SsoTokenStatus.Valid, issued, expiry, "refresh", SsoTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a revoked SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Revoked, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SsoTokenStatus.Revoked, issued, expiry, "refresh", SsoTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an expired SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Expired, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SsoTokenStatus.Expired, issued, expiry, "refresh", SsoTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an invalid SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Invalid, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SsoTokenStatus.Invalid, issued, expiry, "refresh", SsoTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }
}
