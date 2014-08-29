package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.env.{CommonResponder, TestEnv}
import com.blinkbox.books.auth.server.{ZuulAuthorizationException, ZuulTooManyRequestException, Failures, ZuulRequestException}
import spray.http.StatusCodes

import scala.concurrent.duration._

class DefaultPasswordUpdateServiceSpecs extends SpecBase {

  "The password update service" should "report a success if the SSO service accepts a password change request" in new TestEnv with CommonResponder {
    ssoNoContent()

    whenReady(passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) { _ => }
  }

  it should "report a ZuulRequestException if the SSO service doesn't recognize the old password" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Forbidden)

    failingWith[ZuulRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.oldPasswordInvalid)
  }

  it should "report a ZuulRequestException if the SSO service doesn't accept the new password because it doesn't meet requirements" in new TestEnv with CommonResponder {
    ssoInvalidRequest("Password does not meet minimum requirements")

    failingWith[ZuulRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.passwordTooShort)
  }

  it should "report a ZuulTooManyRequestException if the SSO service respond with a too-many-request response" in new TestEnv with CommonResponder {
    ssoTooManyRequests(20)

    failingWith[ZuulTooManyRequestException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserA)) should equal(Failures.tooManyRequests(20.seconds))
  }

  it should "report a ZuulAuthorizationException if the user does not have an SSO access token" in new TestEnv with CommonResponder {
    ssoNoInvocation()

    failingWith[ZuulAuthorizationException](passwordUpdateService.updatePassword("foo", "bar")(authenticatedUserB)) should equal(Failures.unverifiedIdentity)
  }
}
