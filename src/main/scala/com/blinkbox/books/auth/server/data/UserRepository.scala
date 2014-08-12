package com.blinkbox.books.auth.server.data

import com.blinkbox.books.auth.server.{PasswordHasher, UserRegistration}
import com.blinkbox.books.slick.{TablesSupport, SlickTypes}
import com.blinkbox.books.time.{Clock, TimeSupport}

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait UserRepository[Profile <: BasicProfile] extends SlickTypes[Profile] {
  val passwordHasher: PasswordHasher

  def userWithUsernameAndPassword(username: String, password: String)(implicit session: Session): Option[User]
  def userWithUsername(username: String)(implicit session: Session): Option[User]
  def updateUser(user: User)(implicit session: Session): Unit
  def createUser(registration: UserRegistration)(implicit session: Session): User
  def userWithId(id: UserId)(implicit session: Session): Option[User]
}

trait JdbcUserRepository[Profile <: JdbcProfile] extends UserRepository[Profile] with TablesSupport[Profile, ZuulTables[Profile]] {
  this: TimeSupport =>

  import tables._
  import driver.simple._

  override def userWithUsername(username: String)(implicit session: Session): Option[User] =
    users.where(_.username === username).firstOption

  override def userWithUsernameAndPassword(username: String, password: String)(implicit session: Session): Option[User] = {
    val user = users.where(_.username === username).firstOption

    // even if the user isn't found we still need to perform an scrypt hash of something to help
    // prevent timing attacks as this hashing process is the bulk of the request time
    if (user.isEmpty) passwordHasher.hash("random string")

    user.filter(u => passwordHasher.check(password, u.passwordHash))
  }

  override def createUser(reg: UserRegistration)(implicit session: Session): User = {
    val now = clock.now()
    val passwordHash = passwordHasher.hash(reg.password)
    val user = User(UserId.Invalid, now, now, reg.username, reg.firstName, reg.lastName, passwordHash, reg.allowMarketing)

    val id = (users returning users.map(_.id)) += user

    user.copy(id = id)
  }

  override def userWithId(id: UserId)(implicit session: Session) = users.where(_.id === id).list.headOption
  override def updateUser(user: User)(implicit session: Session): Unit = users.where(_.id === user.id).update(user)
}

class DefaultUserRepository[Profile <: JdbcProfile](val tables: ZuulTables[Profile], val passwordHasher: PasswordHasher)(implicit val clock: Clock)
  extends TimeSupport with JdbcUserRepository[Profile]
