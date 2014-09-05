package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidGrant, InvalidClient}
import com.blinkbox.books.auth.server.data.RefreshTokenId
import com.blinkbox.books.auth.server.sso.{SsoUnknownException, SsoUnauthorized}
import com.blinkbox.books.auth.server.{ZuulRequestException, RefreshTokenCredentials}
import com.blinkbox.books.auth.server.env.TestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import spray.http.StatusCodes

class DefaultRefreshTokenServiceSpecs extends SpecBase {

  import env._

  it should "refresh a valid refresh token given the associated client credentials" in {
    ssoSuccessfulAuthentication()

    val refreshFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA1.token, Some(clientInfoA1.client_id), Some("test-secret-a1")))

    whenReady(refreshFuture) { token =>
      token.expires_in should equal(validTokenZuulExpiry)
      token.user_id shouldBe userIdA.external
      token.client_id shouldBe Some(clientInfoA1.client_id)

      import tables._
      import tables.driver.simple._
      val updatedToken = db.withSession { implicit session =>
        tables.refreshTokens.filter(_.id === refreshTokenClientA1.id).firstOption
      }

      updatedToken shouldBe defined
      updatedToken.foreach(_.expiresAt should equal(now.plusDays(90)))
    }
  }

  it should "refresh a valid refresh token even if we don't have an SSO token for that" in {
    ssoNoInvocation()
    removeSSOTokens()

    val refreshFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA1.token, Some(clientInfoA1.client_id), Some("test-secret-a1")))

    whenReady(refreshFuture) { token =>
      token.expires_in should equal(1800) // Please note that in this case this value is not bound to SSO at all
      token.user_id shouldBe userIdA.external
      token.client_id shouldBe Some(clientInfoA1.client_id)

      import tables._
      import tables.driver.simple._
      val updatedToken = db.withSession { implicit session =>
        tables.refreshTokens.filter(_.id === refreshTokenClientA1.id).firstOption
      }

      updatedToken shouldBe defined
      updatedToken.foreach(_.expiresAt should equal(now.plusDays(90)))
    }
  }

  it should "not refresh a valid refresh token and signal an error if wrong client credentials are provided" in {
    ssoSuccessfulAuthentication()

    val refreshFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA2.token, Some(clientInfoA1.client_id), Some("test-secret-a2")))

    failingWith[ZuulRequestException](refreshFuture) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  it should "not refresh an invalid refresh token and signal an error whether or not correct client credentials are provided" in {
    ssoNoInvocation()

    val correctClientFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", Some(clientInfoA1.client_id), Some("test-secret-a1")))

    val noClientFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", None, None))

    (correctClientFuture :: noClientFuture :: Nil) foreach { f =>
      failingWith[ZuulRequestException](f) should matchPattern {
        case ZuulRequestException(_, InvalidGrant, None) =>
      }
    }
  }

  it should "revoke a valid refresh token" in {
    ssoNoContent()

    whenReady(refreshTokenService.revokeRefreshToken(refreshTokenClientA1.token)) { _ =>  }
  }

  it should "signal an error when revoking an invalid refresh token" in {
    ssoNoInvocation()

    failingWith[ZuulRequestException](refreshTokenService.revokeRefreshToken("foo-token")) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "signal an error when revoking an already revoked refresh token" in {
    ssoNoInvocation()

    failingWith[ZuulRequestException](refreshTokenService.revokeRefreshToken(refreshTokenClientA3.token)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "revoke a zuul token even if it doesn't have a corresponding SSO token" in {
    ssoNoInvocation()
    removeSSOTokens()

    whenReady(refreshTokenService.revokeRefreshToken(refreshTokenClientA1.token)) { _ => }
  }

  it should "fail with an exception and not revoke a zuul token if SSO signals an error removing the corresponding SSO token" in {
    ssoResponse(StatusCodes.BadRequest)
    failingWith[SsoUnknownException](refreshTokenService.revokeRefreshToken(refreshTokenClientA1.token))

    val token = db.withSession { implicit session =>
      authRepository.refreshTokenWithId(refreshTokenClientA1Id)
    }

    token map (_.isRevoked) should equal(Some(false))
  }
}

