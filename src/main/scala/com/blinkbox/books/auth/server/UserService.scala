package com.blinkbox.books.auth.server

import java.sql.{SQLIntegrityConstraintViolationException, DataTruncation}

import com.blinkbox.books.auth.server.data.{UserId, Client, UserRepository, User}
import com.blinkbox.books.auth.server.events.{UserUpdated, Publisher}
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait UserService {
  def updateUser(id: UserId, patch: UserPatch): Future[Option[UserInfo]]
  def getUserInfo(id: UserId): Future[Option[UserInfo]]
}

class DefaultUserService[Profile <: BasicProfile, Database <: Profile#Backend#Database]
  (db: Database, repo: UserRepository[Profile], geoIP: GeoIP, events: Publisher)
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

  override def getUserInfo(id: UserId): Future[Option[UserInfo]] = Future {
    db.withSession { implicit session =>
      repo.userWithId(id).map(userInfoFromUser)
    }
  }
}
