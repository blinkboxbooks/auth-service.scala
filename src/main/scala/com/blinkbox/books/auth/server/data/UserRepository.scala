package com.blinkbox.books.auth.server.data

import com.blinkbox.books.auth.server.{PasswordHasher, UserRegistration}
import com.blinkbox.books.slick.{JdbcSupport, SlickSupport}
import com.blinkbox.books.time.{Clock, TimeSupport}

import scala.slick.driver.{JdbcProfile, JdbcDriver}
import scala.slick.profile.BasicProfile

trait UserRepository[Profile <: BasicProfile] extends SlickSupport[Profile] {
  val passwordHasher: PasswordHasher

  def updateUser(user: User)(implicit session: Session): Unit
  def createUser(registration: UserRegistration)(implicit session: Session): User
  def userWithId(id: UserId)(implicit session: Session): Option[User]
}

trait JdbcUserRepository extends UserRepository[JdbcProfile] with ZuulTables {
  this: JdbcSupport with TimeSupport =>
  import driver.simple._

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

class DefaultUserRepository(val tables: ZuulTables, val passwordHasher: PasswordHasher)(implicit val clock: Clock)
  extends TimeSupport with ZuulTablesSupport with JdbcUserRepository
