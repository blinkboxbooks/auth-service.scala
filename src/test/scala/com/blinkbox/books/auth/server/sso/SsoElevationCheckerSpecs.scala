package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.auth.server.{PasswordCredentials, UserRegistration, UserPatch}
import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.spray.InvalidTokenStatusException
import com.blinkbox.books.test.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ShouldMatchers, FunSuite}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SsoElevationCheckerSpecs extends FunSuite with ShouldMatchers with ScalaFutures with FailHelper {
  def ssoMock(status: TokenStatus, expectExtension: Boolean): Sso = new Sso {
    override def updatePassword(token: SsoAccessToken, oldPassword: String, newPassword: String): Future[Unit] = ???
    override def generatePasswordResetToken(username: String): Future[SsoPasswordResetTokenResponse] = ???
    override def tokenStatus(token: SsoToken): Future[TokenStatus] = ???
    override def refresh(ssoRefreshToken: SsoRefreshToken): Future[SsoCredentials] = ???
    override def revokeToken(token: SsoRefreshToken): Future[Unit] = ???
    override def authenticate(c: PasswordCredentials): Future[SsoAuthenticatedCredentials] = ???
    override def resetPassword(passwordToken: SsoPasswordResetToken, newPassword: String): Future[SsoAuthenticatedCredentials] = ???
    override def register(req: UserRegistration): Future[(SsoUserId, SsoCredentials)] = ???
    override def updateUser(token: SsoAccessToken, req: UserPatch): Future[Unit] = ???
    override def userInfo(token: SsoAccessToken): Future[UserInformation] = ???
    override def linkAccount(token: SsoAccessToken, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit] = ???

    override def extendSession(token: SsoAccessToken): Future[Unit] =
      if (expectExtension) Future.successful(()) else Future.failed(new RuntimeException("Unexpected session extension"))

    override def sessionStatus(token: SsoAccessToken): Future[TokenStatus] = Future.successful(status)
  }

  val user = User(1, None, "some-token", Map("sso/at" -> "some-sso-token"))

  def checker(status: TokenStatus, expectExtension: Boolean) = new SsoElevationChecker(ssoMock(status, expectExtension))

  test("A valid token with critical elevation should provide critical elevation") {
    val f = checker(TokenStatus(SsoTokenStatus.Valid, None, None, Some("access"), Some(SsoTokenElevation.Critical), Some(300)), true)(user)
    whenReady(f) { _ should equal(Elevation.Critical) }
  }

  test("A valid unelevated token should provide unelevated response") {
    val f = checker(TokenStatus(SsoTokenStatus.Valid, None, None, Some("access"), Some(SsoTokenElevation.None), None), false)(user)
    whenReady(f) { _ should equal(Elevation.Unelevated) }
  }

  test("An invalid unelevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Invalid, None, None, Some("access"), Some(SsoTokenElevation.None), None), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }

  test("A revoked unelevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Revoked, None, None, Some("access"), Some(SsoTokenElevation.None), None), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }

  test("An expired unelevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Expired, None, None, Some("access"), Some(SsoTokenElevation.None), None), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }

  test("An invalid critically elevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Invalid, None, None, Some("access"), Some(SsoTokenElevation.Critical), Some(300)), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }

  test("A revoked critically elevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Revoked, None, None, Some("access"), Some(SsoTokenElevation.Critical), Some(300)), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }

  test("An expired critically elevated token should result in an exception") {
    val f = checker(TokenStatus(SsoTokenStatus.Expired, None, None, Some("access"), Some(SsoTokenElevation.Critical), Some(300)), false)(user)
    failingWith[InvalidTokenStatusException](f)
  }
}
