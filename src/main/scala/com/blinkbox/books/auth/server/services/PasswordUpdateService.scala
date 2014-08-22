package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.Failures
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.auth.server.sso.{SSOTooManyRequests, SSOInvalidRequest, SSOForbidden, SSO}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}

trait PasswordUpdateService {
  def updatePassword(oldPassword: String, newPassword: String)(implicit user: AuthenticatedUser): Future[Unit]
}

class DefaultPasswordUpdateService(
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock) extends PasswordUpdateService {

  override def updatePassword(oldPassword: String, newPassword: String)(implicit user: AuthenticatedUser): Future[Unit] =
    user.ssoAccessToken.map {
      at => sso.updatePassword(at, oldPassword, newPassword) transform(identity, {
        case SSOForbidden => Failures.oldPasswordIsWrong
        case SSOInvalidRequest(_) => Failures.passwordTooShort
        case SSOTooManyRequests(retryAfter) => Failures.tooManyRequests(retryAfter)
      })
    } getOrElse(Future.failed(Failures.unverifiedIdentity))
}
