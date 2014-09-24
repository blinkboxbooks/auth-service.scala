package com.blinkbox.books.auth.server.data

import com.blinkbox.books.auth.server.sso.SsoUserId
import com.blinkbox.books.auth.server.{PasswordHasher, UserRegistration}
import com.blinkbox.books.slick.{TablesSupport, SlickTypes}
import com.blinkbox.books.time.{Clock, TimeSupport}

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile
import scala.util.Random

trait UserRepository[Profile <: BasicProfile] extends SlickTypes[Profile] {
  def userWithUsername(username: String)(implicit session: Session): Option[User]
  def updateUser(user: User)(implicit session: Session): Unit
  def createUser(registration: UserRegistration)(implicit session: Session): User
  def userWithId(id: UserId)(implicit session: Session): Option[User]
  def userWithSsoId(id: SsoUserId)(implicit session: Session): Option[User]
  def registerUsernameUpdate(oldUsername: String, updatedUser: User)(implicit session: Session): Unit
  def userWithHistoryById(id: UserId)(implicit session: Session): Option[(User, List[PreviousUsername])]
  def userWithHistoryByUsername(username: String)(implicit session: Session): List[(User, List[PreviousUsername])]
  def userWithHistoryByName(firstName: String, lastName: String)(implicit session: Session): List[(User, List[PreviousUsername])]
}

trait JdbcUserRepository[Profile <: JdbcProfile] extends UserRepository[Profile] with TablesSupport[Profile, ZuulTables[Profile]] {
  this: TimeSupport =>

  import tables._
  import driver.simple._

  override def userWithUsername(username: String)(implicit session: Session): Option[User] =
    users.filter(_.username === username).firstOption

  override def createUser(reg: UserRegistration)(implicit session: Session): User = {
    val now = clock.now()

    // TODO: This is not needed any more as SSO is handling password checks; after the migration is done remove this field
    val passwordHash = Random.alphanumeric.take(12).mkString
    val user = User(UserId.Invalid, now, now, reg.username, reg.firstName, reg.lastName, passwordHash, reg.allowMarketing)

    val id = (users returning users.map(_.id)) += user

    user.copy(id = id)
  }

  override def userWithId(id: UserId)(implicit session: Session) = users.filter(_.id === id).firstOption
  override def updateUser(user: User)(implicit session: Session): Unit = users.filter(_.id === user.id).update(user)
  override def userWithSsoId(id: SsoUserId)(implicit session: Session) = users.filter(_.ssoId === id).firstOption

  override def registerUsernameUpdate(oldUsername: String, updatedUser: User)(implicit session: Session): Unit = {
    previousUsernames += PreviousUsername(PreviousUsernameId.invalid, clock.now(), updatedUser.id, oldUsername)
  }

  private def groupByUser(l: List[(User, Option[PreviousUsername])]): List[(User, List[PreviousUsername])] =
    (l.groupBy(_._1).collect {
      case (user, pairs) => (user, pairs.map(_._2).flatten)
    }).toList

  private def userWithHistoryQuery = (for {
    (u, p) <- users leftJoin previousUsernames on(_.id === _.userId)
  } yield (u, p)).sortBy(_._2.createdAt.desc).map { case (u, p) => (u, p.?) }

  override def userWithHistoryById(id: UserId)(implicit session: Session): Option[(User, List[PreviousUsername])] =
    groupByUser(userWithHistoryQuery.filter(_._1.id === id).list).headOption

  def userWithHistoryByUsername(username: String)(implicit session: Session): List[(User, List[PreviousUsername])] = {
    val q = userWithHistoryQuery
    val uq = q.filter(_._1.username === username)
    val u = uq.list
    groupByUser(u)
  }

  def userWithHistoryByName(firstName: String, lastName: String)(implicit session: Session): List[(User, List[PreviousUsername])] =
    groupByUser(userWithHistoryQuery.filter({
      case (u, _) => u.firstName.toLowerCase === firstName.toLowerCase && u.lastName.toLowerCase === lastName.toLowerCase
    }).list)
}

class DefaultUserRepository[Profile <: JdbcProfile](val tables: ZuulTables[Profile])(implicit val clock: Clock)
  extends TimeSupport with JdbcUserRepository[Profile]
