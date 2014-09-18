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
}

class DefaultUserRepository[Profile <: JdbcProfile](val tables: ZuulTables[Profile])(implicit val clock: Clock)
  extends TimeSupport with JdbcUserRepository[Profile]
