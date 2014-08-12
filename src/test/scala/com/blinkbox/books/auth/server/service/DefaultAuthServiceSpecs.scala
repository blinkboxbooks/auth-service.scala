package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidClient, InvalidGrant}
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.env.{AuthenticationTestEnv, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

class DefaultAuthServiceSpecs extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  "The authentication service" should "revoke a valid refresh token" in new TestEnv {
    whenReady(authService.revokeRefreshToken(refreshTokenClientA1.token)) { _ =>  }
  }

  it should "signal an error when revoking an invalid refresh token" in new TestEnv {
    failingWith[ZuulRequestException](authService.revokeRefreshToken("foo-token")) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "signal an error when revoking an already revoked refresh token" in new TestEnv {
    failingWith[ZuulRequestException](authService.revokeRefreshToken(refreshTokenClientA3.token)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "refresh a valid refresh token given the associated client credentials" in new TestEnv {
    cancel("Awaiting SSO implementation")
    val refreshFuture = authService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA1.token, Some(clientInfoA1.client_id), Some("test-secret-a1")))

    whenReady(refreshFuture) { token =>
      token.expires_in should equal(1800)
      token.user_id shouldBe userIdA.external
      token.client_id shouldBe Some(clientInfoA1.client_id)

      import tables._
      import tables.driver.simple._
      val updatedToken = db.withSession { implicit session =>
        tables.refreshTokens.where(_.id === refreshTokenClientA1.id).firstOption
      }

      updatedToken shouldBe defined
      updatedToken.foreach(_.expiresAt should equal(now.plusDays(90)))
    }
  }

  it should "not refresh a valid refresh token and signal an error if wrong client credentials are provided" in new TestEnv {
    cancel("Awaiting SSO implementation")
    val refreshFuture = authService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA2.token, Some(clientInfoA1.client_id), Some("test-secret-a2")))

    failingWith[ZuulRequestException](refreshFuture) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  it should "not refresh an invalid refresh token and signal an error whether or not correct client credentials are provided" in new TestEnv {
    cancel("Awaiting SSO implementation")
    val correctClientFuture = authService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", Some(clientInfoA1.client_id), Some("test-secret-a1")))

    val noClientFuture = authService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", None, None))

    (correctClientFuture :: noClientFuture :: Nil) foreach { f =>
      failingWith[ZuulRequestException](f) should matchPattern {
        case ZuulRequestException(_, InvalidGrant, None) =>
      }
    }
  }
}
