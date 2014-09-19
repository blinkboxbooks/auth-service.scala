package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.{PreviousUsernameInfo, AdminUserInfo}
import com.blinkbox.books.auth.server.data.{PreviousUsername, User, UserRepository, UserId}
import com.blinkbox.books.slick.DatabaseSupport

import scala.concurrent.{ExecutionContext, Future}

sealed trait SearchCriteria
case class IdSearch(id: UserId) extends SearchCriteria
case class NameSearch(firstName: String, lastName: String) extends SearchCriteria
case class UsernameSearch(username: String) extends SearchCriteria

trait AdminUserService {
  def userSearch(c: SearchCriteria): Future[List[AdminUserInfo]]
  def userDetails(id: UserId): Future[Option[AdminUserInfo]]
}

class DefaultAdminUserService[DB <: DatabaseSupport](
    db: DB#Database,
    userRepo: UserRepository[DB#Profile])(implicit ec: ExecutionContext) extends AdminUserService {

  def info(u: (User, List[PreviousUsername])): AdminUserInfo = {
    val (user, history) = u
    AdminUserInfo(
      user_id = user.id.external,
      user_uri = user.id.uri,
      user_username = user.username,
      user_first_name = user.firstName,
      user_last_name = user.lastName,
      user_allow_marketing_communications = user.allowMarketing,
      user_previous_usernames = history.map { prev =>
        PreviousUsernameInfo(prev.username, prev.createdAt)
      }
    )
  }

  override def userSearch(c: SearchCriteria): Future[List[AdminUserInfo]] = Future{
    db.withSession { implicit session =>
      c match {
        case IdSearch(id) => userRepo.userWithHistoryById(id).map(info).toList
        case NameSearch(firstName, lastName) => userRepo.userWithHistoryByName(firstName, lastName).map(info)
        case UsernameSearch(username) => userRepo.userWithHistoryByUsername(username).map(info)
      }
    }
  }

  override def userDetails(id: UserId): Future[Option[AdminUserInfo]] = Future {
    db.withSession { implicit session => userRepo.userWithHistoryById(id).map(info) }
  }
}
