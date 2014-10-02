package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.UserRegistration
import com.blinkbox.books.auth.server.data.{User, UserId, UserRepository}
import com.blinkbox.books.auth.server.events.{Publisher, UserRegistered, UserUpdated}
import com.blinkbox.books.auth.server.sso.{Sso, SsoAccessToken, SsoConflict, UserInformation}
import com.blinkbox.books.slick.DatabaseSupport
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait SsoSyncService extends ((Option[User], SsoAccessToken) => Future[User]) {
  def apply(maybeUser: Option[User], ssoAccessToken: SsoAccessToken): Future[User]
  def apply(ourUser: User, u: UserInformation): Future[User]
}

class DefaultSsoSyncService[DB <: DatabaseSupport](
    db: DB#Database,
    userRepo: UserRepository[DB#Profile],
    termsVersion: String,
    events: Publisher,
    sso: Sso)(implicit ec: ExecutionContext) extends SsoSyncService with StrictLogging {

  private def registerUser(reg: UserRegistration): Future[User] = Future {
    db.withSession { implicit session => userRepo.createUser(reg) }
  }

  private def updateUser(user: User): Future[Unit] = Future {
    db.withSession { implicit session => userRepo.updateUser(user) }
  }

  private def syncNewUser(ssoAccessToken: SsoAccessToken): Future[User] = {
    val registrationFuture = sso.userInfo(ssoAccessToken).map { u =>
      (u.userId, UserRegistration(u.firstName, u.lastName, u.username, Random.nextString(32), true, false, None, None, None, None))
    }

    for {
      (ssoId, registration) <- registrationFuture
      user <- registerUser(registration)
      _ <- sso linkAccount(ssoAccessToken, user.id, false, termsVersion)
      linkedUser = user.copy(ssoId = Some(ssoId))
      _ <- updateUser(linkedUser)
      _ <- events.publish(UserRegistered(linkedUser))
    } yield linkedUser
  }

  private def checkUsernameUpdate(oldUser: User, newUser: User): Future[Unit] =
    if (oldUser.username == newUser.username) Future.successful(())
    else Future {
      db.withSession { implicit session =>
        userRepo.registerUsernameUpdate(oldUser.username, newUser)
      }
    }

  private def syncExistingUser(ssoAccessToken: SsoAccessToken, user: User): Future[User] =
    sso.userInfo(ssoAccessToken).flatMap(apply(user, _))

  private def logConflict(id: UserId): PartialFunction[Throwable, Unit] = {
    case SsoConflict => logger.warn("User {} is already linked on SSO", id.external)
  }

  override def apply(maybeUser: Option[User], ssoAccessToken: SsoAccessToken): Future[User] =
    maybeUser.fold(syncNewUser(ssoAccessToken)) { user =>
      if (user.ssoId.isDefined) syncExistingUser(ssoAccessToken, user)
      else for {
        _           <- sso linkAccount(ssoAccessToken, user.id, user.allowMarketing, termsVersion) recover(logConflict(user.id))
        updatedUser <- syncExistingUser(ssoAccessToken, user)
      } yield updatedUser
    }

  override def apply(ourUser: User, ssoUser: UserInformation): Future[User] = {
    val updatedUser = ourUser.copy(
        firstName = ssoUser.firstName,
        lastName = ssoUser.lastName,
        username = ssoUser.username,
        ssoId = Some(ssoUser.userId))
   
    if (updatedUser != ourUser) {
      for {
        _ <- checkUsernameUpdate(ourUser, updatedUser)
        _ <- updateUser(updatedUser)
        _ <- events.publish(UserUpdated(ourUser, updatedUser))
      } yield updatedUser
    } else Future.successful(ourUser)
  }
}
