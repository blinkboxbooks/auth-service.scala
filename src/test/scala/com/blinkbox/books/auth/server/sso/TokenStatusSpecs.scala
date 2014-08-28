package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.TokenStatusEnv
import com.blinkbox.books.testkit.FailHelper
import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.{Matchers, FlatSpec}

class TokenStatusSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  def checkDates(issued: DateTime, expiry: DateTime) =
    issued == new DateTime("2000-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC) &&
    expiry == new DateTime("2020-01-01T01:01:01.010Z").withZone(DateTimeZone.UTC)

  "The SSO client" should "return session info for a valid, critically elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    whenReady(sso.sessionStatus(SSOAccessToken("an-acces-token")))(_ should matchPattern {
      case SessionStatus(SSOTokenStatus.Valid, issued, expiry, "refresh", SSOTokenElevation.Critical, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a valid non-elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None)

    whenReady(sso.sessionStatus(SSOAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SSOTokenStatus.Valid, issued, expiry, "refresh", SSOTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for a revoked SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Revoked, SSOTokenElevation.None)

    whenReady(sso.sessionStatus(SSOAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SSOTokenStatus.Revoked, issued, expiry, "refresh", SSOTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an expired SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Expired, SSOTokenElevation.None)

    whenReady(sso.sessionStatus(SSOAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SSOTokenStatus.Expired, issued, expiry, "refresh", SSOTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }

  it should "return session info for an invalid SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Invalid, SSOTokenElevation.None)

    whenReady(sso.sessionStatus(SSOAccessToken("an-access-token")))(_ should matchPattern {
      case SessionStatus(SSOTokenStatus.Invalid, issued, expiry, "refresh", SSOTokenElevation.None, 300) if checkDates(issued, expiry) =>
    })
  }
}
