package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.env.{PasswordResetEnv, AuthenticationTestEnv}
import com.blinkbox.books.auth.server.events.UserPasswordResetRequested
import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import com.blinkbox.books.schemas.events.user.v2.User.PasswordResetRequested
import spray.http.{HttpEntity, FormData, StatusCodes}

class PasswordResetSpecs extends ApiSpecBase[PasswordResetEnv] {

  override def newEnv = new PasswordResetEnv

  "The service" should "generate a password reset email and respond with an empty 200 response if SSO responds with a success" in {
    env.ssoGenerateResetToken
    env.preSyncUser(env.userIdA)

    Post("/password/reset", FormData(Map("username" -> env.userA.username))) ~> route ~> check {
      status should equal(StatusCodes.OK)
      response.entity should equal(HttpEntity.Empty)

      env.publisher.events should matchPattern {
        case UserPasswordResetRequested(name, _, _) :: Nil if name == env.userA.username =>
      }
    }
  }

  it should "not generate a password reset email but still respond with an empty 200 response if SSO responds with a not-found" in {
    env.ssoUserNotFound
    env.preSyncUser(env.userIdA)

    Post("/password/reset", FormData(Map("username" -> env.userA.username))) ~> route ~> check {
      status should equal(StatusCodes.OK)
      response.entity should equal(HttpEntity.Empty)

      env.publisher.events shouldBe empty
    }
  }

  it should "accept the password change request given that the password reset token is recognized by SSO" in {
    env.ssoSuccessfulAuthentication()
    env.ssoSuccessfulUserAInfo()
    env.preSyncUser(env.userIdA)

    Post("/oauth2/token", FormData(Map(
      "grant_type" -> "urn:blinkbox:oauth:grant-type:password-reset-token",
      "password_reset_token" -> "res3tt0ken",
      "password" -> "a-password"
    ))) ~> route ~> check {
      status should equal(StatusCodes.OK)

      val u = env.userA

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, Some(_), ExternalUserId(_), userUriExpr(_), u.username, u.firstName, u.lastName,
        None, None, None, None, None, None, None, None) =>
      }
    }
  }

  it should "reject the password change request and answer with a BadRequest given that the password reset token is not recognized by SSO" in {
    env.ssoUnsuccessfulAuthentication()
    env.preSyncUser(env.userIdA)

    Post("/oauth2/token", FormData(Map(
      "grant_type" -> "urn:blinkbox:oauth:grant-type:password-reset-token",
      "password_reset_token" -> "res3tt0ken",
      "password" -> "a-password"
    ))) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)
      responseAs[ZuulRequestException] should equal(Failures.invalidPasswordResetToken)
    }
  }

  it should "validate a password reset token given that it is recognized by SSO" in {
    env.ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None, "password_reset")
    env.preSyncUser(env.userIdA)

    Post("/password/reset/validate-token", FormData(Map(
      "grant_type" -> "urn:blinkbox:oauth:grant-type:password-reset-token",
      "password_reset_token" -> "res3tt0ken",
      "password" -> "a-password"
    ))) ~> route ~> check {
      status should equal(StatusCodes.OK)
      response.entity should equal(HttpEntity.Empty)
    }
  }

  it should "not validate a password reset token and answer with a BadRequest given that it is not recognized by SSO" in {
    env.ssoSessionInfo(SSOTokenStatus.Invalid, SSOTokenElevation.None, "password_reset")
    env.preSyncUser(env.userIdA)

    Post("/password/reset/validate-token", FormData(Map(
      "grant_type" -> "urn:blinkbox:oauth:grant-type:password-reset-token",
      "password_reset_token" -> "res3tt0ken",
      "password" -> "a-password"
    ))) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)
      responseAs[ZuulRequestException] should equal(Failures.invalidPasswordResetToken)
    }
  }
}
