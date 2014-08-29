package com.blinkbox.books.auth.server.services

import java.util.NoSuchElementException

import com.blinkbox.books.auth.server.UserRegistration
import com.blinkbox.books.auth.server.data.{UserRepository, User}
import com.blinkbox.books.auth.server.events.{UserUpdated, Publisher, UserRegistered}
import com.blinkbox.books.auth.server.sso.{SSO, SSOAccessToken}
import com.blinkbox.books.slick.DatabaseSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait SsoSyncService extends ((Option[User], SSOAccessToken) => Future[User]) {
  def apply(maybeUser: Option[User], ssoAccessToken: SSOAccessToken): Future[User]
}

class DefaultSsoSyncService[DB <: DatabaseSupport](
    db: DB#Database,
    userRepo: UserRepository[DB#Profile],
    events: Publisher,
    sso: SSO)(implicit ec: ExecutionContext) extends SsoSyncService {

  // TODO: Move this to configuration
  val TermsVersion = "1.0"

  private def registerUser(reg: UserRegistration): Future[User] = Future {
    db.withSession { implicit session => userRepo.createUser(reg) }
  }

  private def updateUser(user: User): Future[Unit] = Future {
    db.withSession { implicit session => userRepo.updateUser(user) }
  }

  private def syncNewUser(ssoAccessToken: SSOAccessToken): Future[User] = {
    val registrationFuture = sso.userInfo(ssoAccessToken).map { u =>
      (u.userId, UserRegistration(u.firstName, u.lastName, u.username, Random.nextString(32), true, false, None, None, None, None))
    }

    for {
      (ssoId, registration) <- registrationFuture
      user <- registerUser(registration)
      _ <- sso linkAccount(ssoAccessToken, user.id, false, TermsVersion)
      linkedUser = user.copy(ssoId = Some(ssoId))
      _ <- updateUser(linkedUser)
      _ <- events.publish(UserRegistered(linkedUser))
    } yield linkedUser
  }

  private def syncExistingUser(ssoAccessToken: SSOAccessToken, user: User): Future[User] = {
    val userFuture = sso.userInfo(ssoAccessToken).map { u =>
      user.copy(
        firstName = u.firstName,
        lastName = u.lastName,
        username = u.username,
        ssoId = Some(u.userId))
    }

    (for {
      updatedUser <- userFuture if updatedUser != user
      _           <- updateUser(updatedUser)
      _           <- events.publish(UserUpdated(user, updatedUser))
    } yield updatedUser) recover { case _: NoSuchElementException => user }
  }

  override def apply(maybeUser: Option[User], ssoAccessToken: SSOAccessToken): Future[User] =
    maybeUser.fold(syncNewUser(ssoAccessToken)) { user =>
      if (user.ssoId.isDefined) syncExistingUser(ssoAccessToken, user)
      else for {
        _           <- sso linkAccount(ssoAccessToken, user.id, user.allowMarketing, TermsVersion)
        updatedUser <- syncExistingUser(ssoAccessToken, user)
      } yield updatedUser
    }
}
