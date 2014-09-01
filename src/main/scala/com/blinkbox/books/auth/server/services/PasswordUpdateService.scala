package com.blinkbox.books.auth.server.services

import java.net.URL
import java.util.NoSuchElementException

import com.blinkbox.books.auth.server.data.{Client => UserClient, _}
import com.blinkbox.books.auth.server.events.{UserPasswordResetRequested, Publisher}
import com.blinkbox.books.auth.server.sso._
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.slick.DatabaseSupport
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}

trait PasswordUpdateService {
  def updatePassword(oldPassword: String, newPassword: String)(implicit user: AuthenticatedUser): Future[Unit]
  def generatePasswordResetToken(username: String): Future[Unit]
  def resetPassword(credentials: ResetTokenCredentials): Future[TokenInfo]
  def validatePasswordResetToken(resetToken: SsoPasswordResetToken): Future[Unit]
}

class DefaultPasswordUpdateService[DB <: DatabaseSupport](
    db: DB#Database,
    userRepo: UserRepository[DB#Profile],
    authRepo: AuthRepository[DB#Profile],
    tokenBuilder: TokenBuilder,
    ssoSync: SsoSyncService,
    events: Publisher,
    sso: Sso)(implicit executionContext: ExecutionContext, clock: Clock) extends PasswordUpdateService with ClientAuthenticator[DB#Profile] {

  // TODO: Make this configurable
  private def resetUrl(token: SsoPasswordResetToken) = new URL(s"http://foo.bar/password-reset/${token.value}")

  private def userBySsoId(ssoId: SsoUserId): Future[Option[User]] = Future {
    db.withSession { implicit session => userRepo.userWithSsoId(ssoId) }
  }

  // TODO: Remove this duplication of code
  private def authenticateClient(credentials: ClientCredentials, id: UserId): Future[Option[UserClient]] = Future {
    db.withSession { implicit session => authenticateClient(authRepo, credentials, id) }
  }

  // TODO: Remove this duplication of code from PasswordAuthenticationService
  private def createRefreshToken(userId: UserId, clientId: Option[ClientId], ssoRefreshToken: SsoRefreshToken) : Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.createRefreshToken(userId, clientId, ssoRefreshToken) }
  }

  override def updatePassword(oldPassword: String, newPassword: String)(implicit user: AuthenticatedUser): Future[Unit] =
    user.ssoAccessToken.map {
      at => sso.updatePassword(at, oldPassword, newPassword) transform(identity, {
        case SsoForbidden => Failures.oldPasswordInvalid
        case SsoInvalidRequest(_) => Failures.passwordTooShort
        case SsoTooManyRequests(retryAfter) => Failures.tooManyRequests(retryAfter)
      })
    } getOrElse (Future.failed(Failures.unverifiedIdentity))

  override def generatePasswordResetToken(username: String): Future[Unit] =
    (for {
      token <- sso generatePasswordResetToken (username)
      res   <- events.publish(UserPasswordResetRequested(username, token.resetToken, resetUrl(token.resetToken)))
    } yield res) recover { case SsoNotFound => ()}

  override def resetPassword(credentials: ResetTokenCredentials): Future[TokenInfo] = {
    val tokenInfo = for {
      SsoUserCredentials(ssoId, ssoCredentials) <- sso resetPassword(credentials.resetToken, credentials.newPassword)
      user                                      <- userBySsoId(ssoId)
      syncedUser                                <- ssoSync(user, ssoCredentials.accessToken)
      client                                    <- authenticateClient(credentials, syncedUser.id)
      refreshToken                              <- createRefreshToken(syncedUser.id, client.map(_.id), ssoCredentials.refreshToken)
    } yield tokenBuilder.issueAccessToken(syncedUser, client, refreshToken, Some(ssoCredentials), includeRefreshToken = true)

    tokenInfo transform(identity, { case SsoUnauthorized => Failures.invalidPasswordResetToken })
  }

  override def validatePasswordResetToken(resetToken: SsoPasswordResetToken): Future[Unit] = sso.tokenStatus(resetToken) filter { s =>
    s.tokenType == "password_reset" && s.status == SsoTokenStatus.Valid
  } transform(_ => (), { case _: NoSuchElementException => Failures.invalidPasswordResetToken })
}
