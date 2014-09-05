package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.events.UserPasswordResetRequested
import com.blinkbox.books.auth.server.sso._
import spray.http.StatusCodes

import scala.concurrent.duration._

class DefaultPasswordUpdateServiceSpecs extends SpecBase {

  import env._

  "The password change endpoint" should "report a success if the SSO service accepts a password change request" in {
    ssoNoContent()

    whenReady(passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) { _ => }
  }

  it should "report a ZuulRequestException if the SSO service doesn't recognize the old password" in {
    ssoResponse(StatusCodes.Forbidden)

    failingWith[ZuulRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.oldPasswordInvalid)
  }

  it should "report a ZuulRequestException if the SSO service doesn't accept the new password because it doesn't meet requirements" in {
    ssoInvalidRequest("Password does not meet minimum requirements")

    failingWith[ZuulRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.passwordTooShort)
  }

  it should "report a ZuulTooManyRequestException if the SSO service respond with a too-many-request response" in {
    ssoTooManyRequests(20)

    failingWith[ZuulTooManyRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.tooManyRequests(20.seconds))
  }

  it should "report a ZuulAuthorizationException if the user does not have an SSO access token" in {
    failingWith[ZuulAuthorizationException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserB)) should equal(Failures.unverifiedIdentity)
  }

  "The reset-token generation function" should "provide a success and send an email for a successful SSO response" in {
    ssoGenerateResetToken

    whenReady(passwordUpdateService.generatePasswordResetToken("foo@bar.baz")) { _ =>
      publisher.events should matchPattern { case UserPasswordResetRequested("foo@bar.baz", SsoPasswordResetToken("r3sett0ken"), _) :: Nil => }
    }
  }

  it should "provide a success but not send any email for an unsuccessful SSO response" in {
    ssoUserNotFound

    whenReady(passwordUpdateService.generatePasswordResetToken("foo@bar.baz")) { _ =>
      publisher.events shouldBe empty
    }
  }

  "The password reset function" should "return client-specific authentication credentials if SSO accepts the given reset token" in {
    ssoSuccessfulAuthentication()
    ssoSuccessfulUserAInfo()
    preSyncUser(userIdA)

    whenReady(passwordUpdateService.resetPassword(resetCredentials)) { token =>
      token.user_id should equal(userIdA.external)
      token.client_id should equal(Some(clientIdA1.external))
      token.refresh_token shouldBe defined
    }
  }

  it should "raise a ZuulRequestException if SSO doesn't accept the provided reset token" in {
    ssoUnsuccessfulAuthentication()

    failingWith[ZuulRequestException](passwordUpdateService.resetPassword(resetCredentials)) should equal(Failures.invalidPasswordResetToken)
  }

  "The password reset token validation function" should "return a successful future if SSO validates the given reset token" in {
    ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.None, "password_reset")

    whenReady(passwordUpdateService.validatePasswordResetToken(SsoPasswordResetToken("res3tt0ken"))) { _ => }
  }

  it should "raise a ZuulRequestException if SSO signals an invalid token" in {
    ssoSessionInfo(SsoTokenStatus.Invalid, SsoTokenElevation.None, "password_reset")

    val ex = failingWith[ZuulRequestException](passwordUpdateService.validatePasswordResetToken(SsoPasswordResetToken("res3tt0ken")))
    ex should equal(Failures.invalidPasswordResetToken)
  }

  it should "raise a ZuulRequestException if the token is not a password_reset in SSO response" in {
    ssoSessionInfo(SsoTokenStatus.Invalid, SsoTokenElevation.None, "refresh")

    val ex = failingWith[ZuulRequestException](passwordUpdateService.validatePasswordResetToken(SsoPasswordResetToken("res3tt0ken")))
    ex should equal(Failures.invalidPasswordResetToken)
  }
}
