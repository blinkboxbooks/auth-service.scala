package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.data.{User, UserId, UserRepository}
import com.blinkbox.books.auth.server.events.{Publisher, UserUpdated}
import com.blinkbox.books.auth.server.sso.{SsoUnauthorized, UserInformation, SsoAccessToken, Sso}
import com.blinkbox.books.auth.server.{ZuulUnknownException, Failures, UserInfo, UserPatch}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.slick.profile.BasicProfile
import scala.util.{Failure, Success}

trait UserService {
  def updateUser(patch: UserPatch)(implicit user: AuthenticatedUser): Future[Option[UserInfo]]
  def getUserInfo()(implicit user: AuthenticatedUser): Future[Option[UserInfo]]
}

class DefaultUserService[Profile <: BasicProfile, Database <: Profile#Backend#Database]
  (db: Database, repo: UserRepository[Profile], sso: Sso, events: Publisher)
  (implicit ec: ExecutionContext, clock: Clock) extends UserService with UserInfoFactory {

  private def updateLocalUser(user: User, patch: UserPatch): UserInfo = {
    val updatedUser = user.copy(
      firstName = patch.first_name.getOrElse(user.firstName),
      lastName = patch.last_name.getOrElse(user.lastName),
      username = patch.username.getOrElse(user.username),
      allowMarketing = patch.allow_marketing_communications.getOrElse(user.allowMarketing)
    )

    if (updatedUser != user) {
      db.withSession { implicit session => repo.updateUser(updatedUser)}
      events.publish(UserUpdated(user, updatedUser))
    }

    if (updatedUser.username != user.username) {
      db.withSession { implicit session => repo.registerUsernameUpdate(user.username, updatedUser) }
    }

    userInfoFromUser(updatedUser)
  }

  override def updateUser(patch: UserPatch)(implicit user: AuthenticatedUser): Future[Option[UserInfo]] = user.ssoAccessToken.map(SsoAccessToken.apply) map { at =>
    getSyncedUser(UserId(user.id), at) flatMap { userOpt =>
      userOpt map { user =>
        val p = Promise[Option[UserInfo]]()

        sso.updateUser(at, patch).onComplete {
          case Success(_) => p.success(Some(updateLocalUser(user, patch)))
          case Failure(ex) => p.failure(ZuulUnknownException("Cannot update sso user", Some(ex)))
        }

        p.future
      } getOrElse Future.successful(None)
    }
  } getOrElse Future.failed(Failures.unverifiedIdentity)

  private def userFromDb(id: UserId): Future[Option[User]] = Future {
    db.withSession { implicit session =>
      repo.userWithId(id)
    }
  }

  private def userFromSso(token: SsoAccessToken): Future[Option[UserInformation]] =
    sso.userInfo(token).map(Option.apply).recover { case SsoUnauthorized => None }

  private def getSyncedUser(id: UserId, token: SsoAccessToken): Future[Option[User]] = {
    val dbFutureOption = userFromDb(id)
    val ssoFutureOption = userFromSso(token)

    for {
      dbOption <- dbFutureOption
      ssoOption <- ssoFutureOption
    } yield {

      (dbOption, ssoOption) match {
        case (None, Some(_)) => throw Failures.unverifiedIdentity
        case (Some(dbU), Some(ssoU)) =>
          val updatedUser = dbU.copy(
            firstName = ssoU.firstName,
            lastName = ssoU.lastName,
            username = ssoU.username,
            ssoId = Some(ssoU.userId)
          )

          if (dbU != updatedUser) {
            db.withSession { implicit session => repo.updateUser(updatedUser)}
            events.publish(UserUpdated(dbU, updatedUser))
          }

          Option(updatedUser)
        case (_, None) => throw Failures.unknownError("The user doesn't exist on the SSO service")
      }
    }
  }

  override def getUserInfo()(implicit user: AuthenticatedUser): Future[Option[UserInfo]] =
    user.ssoAccessToken.map(SsoAccessToken.apply) map { at =>
      getSyncedUser(UserId(user.id), at).map(_.map(userInfoFromUser))
    } getOrElse Future.failed(Failures.unverifiedIdentity)
}
