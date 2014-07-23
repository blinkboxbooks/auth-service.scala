package com.blinkbox.books.auth.server

import java.sql.{SQLIntegrityConstraintViolationException, DataTruncation}

import com.blinkbox.books.auth.server.data.{Client, UserRepository, User}
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.time.Clock
import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database

import scala.concurrent.{ExecutionContext, Future}

trait UserService {
  def updateUser(id: Int, patch: UserPatch): Future[Option[UserInfo]]
  def getUserInfo(id: Int): Future[Option[UserInfo]]
}

class DefaultUserService(db: Database, repo: UserRepository[JdbcProfile], geoIP: GeoIP, events: Publisher)
                        (implicit ec: ExecutionContext, clock: Clock) extends UserService with UserInfoFactory {

  override def updateUser(id: Int, patch: UserPatch): Future[Option[UserInfo]] = Future {
    db.withSession { implicit session =>
      val updatedUser = repo.userWithId(id).map { user =>
        user.copy(
          firstName = patch.first_name.getOrElse(user.firstName),
          lastName = patch.last_name.getOrElse(user.lastName),
          username = patch.username.getOrElse(user.username),
          allowMarketing = patch.allow_marketing_communications.getOrElse(user.allowMarketing)
        )
      }

      updatedUser foreach repo.updateUser

      updatedUser map userInfoFromUser
    }
  }

  override def getUserInfo(id: Int): Future[Option[UserInfo]] = Future {
    db.withSession { implicit session =>
      repo.userWithId(id).map(userInfoFromUser)
    }
  }
}
