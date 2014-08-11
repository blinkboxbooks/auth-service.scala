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
}
