package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidGrant, InvalidClient}
import com.blinkbox.books.auth.server.{ZuulRequestException, RefreshTokenCredentials}
import com.blinkbox.books.auth.server.env.{CommonResponder, AuthenticationTestEnv, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

class DefaultRefreshTokenServiceSpecs extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  it should "refresh a valid refresh token given the associated client credentials" in new AuthenticationTestEnv {
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
        tables.refreshTokens.where(_.id === refreshTokenClientA1.id).firstOption
      }

      updatedToken shouldBe defined
      updatedToken.foreach(_.expiresAt should equal(now.plusDays(90)))
    }
  }

  it should "not refresh a valid refresh token and signal an error if wrong client credentials are provided" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()

    val refreshFuture = refreshTokenService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA2.token, Some(clientInfoA1.client_id), Some("test-secret-a2")))

    failingWith[ZuulRequestException](refreshFuture) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  it should "not refresh an invalid refresh token and signal an error whether or not correct client credentials are provided" in new TestEnv with CommonResponder {
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
}

