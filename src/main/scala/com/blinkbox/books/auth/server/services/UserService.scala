package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.data.{User, UserId, UserRepository}
import com.blinkbox.books.auth.server.events.{Publisher, UserUpdated}
import com.blinkbox.books.auth.server.sso.{SSOUnauthorized, UserInformation, SSOAccessToken, SSO}
import com.blinkbox.books.auth.server.{ZuulUnknownException, Failures, UserInfo, UserPatch}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock

import shapeless.Typeable._
import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait UserService {
  def updateUser(id: UserId, patch: UserPatch): Future[Option[UserInfo]]
  def getUserInfo()(implicit user: AuthenticatedUser): Future[Option[UserInfo]]
}

class DefaultUserService[Profile <: BasicProfile, Database <: Profile#Backend#Database]
  (db: Database, repo: UserRepository[Profile], sso: SSO, events: Publisher)
  (implicit ec: ExecutionContext, clock: Clock) extends UserService with UserInfoFactory {

  override def updateUser(id: UserId, patch: UserPatch): Future[Option[UserInfo]] = Future {
    db.withSession { implicit session =>
      val userUpdate = repo.userWithId(id).map { user =>
        (user, user.copy(
          firstName = patch.first_name.getOrElse(user.firstName),
          lastName = patch.last_name.getOrElse(user.lastName),
          username = patch.username.getOrElse(user.username),
          allowMarketing = patch.allow_marketing_communications.getOrElse(user.allowMarketing)
        ))
      }

      userUpdate foreach { case (oldUser, newUser) =>
        repo.updateUser(newUser)
        events.publish(UserUpdated(oldUser, newUser))
      }

      userUpdate map { case (_, u) => userInfoFromUser(u) }
    }
  }

  def userFromDb(user: AuthenticatedUser): Future[Option[User]] = Future {
    db.withSession { implicit session =>
      repo.userWithId(UserId(user.id))
    }
  }

  def userFromSso(user: AuthenticatedUser): Future[Option[UserInformation]] =
    user.
      claims.
      get("sso/at").
      flatMap(_.cast[String]).
      map(SSOAccessToken.apply).
      map { at =>
        sso.userInfo(at).map(Option.apply).recover { case SSOUnauthorized => None }
      } getOrElse(Future.failed(Failures.unverifiedIdentity))

  override def getUserInfo()(implicit user: AuthenticatedUser): Future[Option[UserInfo]] = {
    val dbFutureOption = userFromDb(user)
    val ssoFutureOption = userFromSso(user)

    for {
      dbOption <- dbFutureOption
      ssoOption <- ssoFutureOption
    } yield {

      val user = (dbOption, ssoOption) match {
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
        case (_, None) => throw ZuulUnknownException("The user doesn't exist on the SSO service")
      }

      user map(userInfoFromUser)
    }
  }
}
