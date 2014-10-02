package com.blinkbox.books.auth.server.sso

import org.joda.time.{DateTime, DateTimeZone}

class SessionStatusSpecs extends SpecBase {

  import env._

  def checkDates(issued: Option[DateTime], expiry: Option[DateTime]) =
    issued == Some(new DateTime("2000-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC)) &&
    expiry == Some(new DateTime("2020-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC))

  "The SSO client" should "return session info for a valid, critically elevated SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical)

    whenReady(sso.sessionStatus(SsoAccessToken("an-acces-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Valid, issued, expiry, Some("refresh"), Some(SsoTokenElevation.Critical), Some(300)) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a valid non-elevated SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Valid, issued, expiry, Some("refresh"), Some(SsoTokenElevation.None), Some(300)) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a revoked SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Revoked, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Revoked, issued, expiry, Some("refresh"), Some(SsoTokenElevation.None), Some(300)) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an expired SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Expired, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Expired, issued, expiry, Some("refresh"), Some(SsoTokenElevation.None), Some(300)) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an invalid SSO token" in {
    ssoSessionInfo(SsoTokenStatus.Invalid, SsoTokenElevation.None)

    whenReady(sso.sessionStatus(SsoAccessToken("an-access-token")))(_ should matchPattern {
      case TokenStatus(SsoTokenStatus.Invalid, issued, expiry, Some("refresh"), Some(SsoTokenElevation.None), Some(300)) if checkDates(issued, expiry) =>
    })
  }
}
