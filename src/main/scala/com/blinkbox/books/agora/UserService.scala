package com.blinkbox.books.agora

import com.blinkbox.books.auth.User
import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.spray._
import com.blinkbox.books.spray.v1.ListPage
import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver.simple._
import java.sql.Date
import com.lambdaworks.crypto.{SCryptUtil, SCrypt}
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import java.security.SecureRandom
import com.blinkbox.security.jwt.util.Base64


// TODO: Use a proper database

trait UserService {
  def create(registration: Registration): Future[TokenInfo]
//  def list(page: Page)(implicit user: User): Future[ListPage[User]]
//  def getById(id: Int)(implicit user: User): Future[Option[User]]
//  def update(id: Int, patch: UserPatch)(implicit user: User): Future[Option[User]]
//  def delete(id: Int)(implicit user: User): Future[Unit]
}

case class UserRow(id: Int, createdAt: Date, updatedAt: Date, username: String, firstName: String, lastName: String, passwordHash: String, allowMarketing: Boolean) {
  //implicit def toUser = User(s"urn:blinkbox:id:user:$id", id, name)
}
object UserRow {
  import scala.language.implicitConversions
  implicit def fromNewUser(r: Registration) = {
    val now = new Date(System.currentTimeMillis())
    val h = SCryptUtil.scrypt(r.password, 16384, 8, 1)
    UserRow(-1, now, now, r.username, r.firstName, r.lastName, h, r.allowMarketing)
  }
}

class Users(tag: Tag) extends Table[UserRow](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
  def createdAt = column[Date]("created_at", O.NotNull)
  def updatedAt = column[Date]("updated_at", O.NotNull)
  def username = column[String]("username", O.NotNull)
  def firstName = column[String]("first_name", O.NotNull)
  def lastName = column[String]("last_name", O.NotNull)
  def passwordHash = column[String]("password_hash", O.NotNull)
  def allowMarketing = column[Boolean]("allow_marketing_communications", O.NotNull)
  def * = (id, createdAt, updatedAt, username, firstName, lastName, passwordHash, allowMarketing) <> ((UserRow.apply _).tupled, UserRow.unapply)
  def indexOnUsername = index("index_users_on_username", username, unique = true)
}

//
//user_id` int(11) DEFAULT NULL,
//`name` varchar(50) DEFAULT NULL,
//`model` varchar(50) DEFAULT NULL,
//`client_secret` varchar(50) DEFAULT NULL,
//`deregistered` tinyint(1) DEFAULT '0',
//`brand` varchar(50) DEFAULT NULL,
//`os` varchar(50) DEFAULT NULL,
//PRIMARY KEY (`id`),
//KEY `index_clients_on_user_id` (`user_id`)

case class ClientRow(id: Int, createdAt: Date, updatedAt: Date, userId: Int, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean) {

}

object ClientRow {
  def create(userId: Int, r: Registration): ClientRow = {
    val now = new Date(System.currentTimeMillis())
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val secret = Base64.encode(buf)
    ClientRow(-1, now, now, userId, r.clientName.get, r.clientBrand.get, r.clientModel.get, r.clientOS.get, secret, false)
  }
}

class Clients(tag: Tag) extends Table[ClientRow](tag, "clients") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
  def createdAt = column[Date]("created_at", O.NotNull)
  def updatedAt = column[Date]("updated_at", O.NotNull)
  def userId = column[Int]("user_id", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def brand = column[String]("brand", O.NotNull)
  def model = column[String]("model", O.NotNull)
  def os = column[String]("os", O.NotNull)
  def secret = column[String]("client_secret", O.NotNull)
  def isDeregistered = column[Boolean]("deregistered", O.NotNull)
  def * = (id, createdAt, updatedAt, userId, name, brand, model, os, secret, isDeregistered) <> ((ClientRow.apply _).tupled, ClientRow.unapply)
  def indexOnUserId = index("index_users_on_username", userId)
}

case class UserAlreadyExists(message: String) extends Exception(message)

class DefaultUserService(config: DatabaseConfig)(implicit executionContext: ExecutionContext) extends UserService {
  val db = Database.forURL("jdbc:" + config.uri.withUserInfo("").toString, driver="com.mysql.jdbc.Driver", user = "zuul", password = "mypass")
  val users = TableQuery[Users]
  val clients = TableQuery[Clients]

  def create(registration: Registration): Future[TokenInfo] = Future {
    val (user, client) = db.withTransaction { implicit transaction =>
      val userId = (users returning users.map(_.id)) += registration
      val u = users.where(_.id === userId).first
      val c = if (registration.clientName.isDefined && registration.clientBrand.isDefined && registration.clientModel.isDefined && registration.clientOS.isDefined) {
        val clientId = (clients returning clients.map(_.id)) += ClientRow.create(userId, registration)
        clients.where(_.id === clientId).firstOption
      } else None
      (u, c)
    }
    TokenInfo(
      access_token = "the access token",
      token_type = "bearer",
      expires_in = 1800,
      refresh_token = Some("a refresh token"),
      user_id = s"urn:blinkbox:zuul:user:${user.id}",
      user_uri = s"/users/${user.id}",
      user_username = user.username,
      user_first_name = user.firstName,
      user_last_name = user.lastName,
      client_id = client.map(row => s"urn:blinkbox:zuul:client:${row.id}"),
      client_uri = client.map(row => s"/clients/${row.id}"),
      client_name = client.map(_.name),
      client_brand = client.map(_.brand),
      client_model = client.map(_.model),
      client_os = client.map(_.os),
      client_secret = client.map(_.secret))
  } recoverWith {
    case e: MySQLIntegrityConstraintViolationException => Future.failed(UserAlreadyExists(e.getMessage))
  }

//  def list(page: Page)(implicit user: User): Future[ListPage[User]] = Future {
//    db.withSession { implicit session =>
//      val q = users.where(_.userId === user.id)
//      val items = q.drop(page.offset).take(page.count).list
//      val numberOfResults = Query(q.length).first
//      ListPage(numberOfResults, page.offset, items.length, items.map(_.toUser))
//    }
//  }
//
//  def getById(id: Int)(implicit user: User): Future[Option[User]] = Future {
//    db.withSession { implicit session =>
//      users.where(r => r.id === id && r.userId === user.id).firstOption.map(_.toUser)
//    }
//  }
//
//  def update(id: Int, patch: UserPatch)(implicit user: User): Future[Option[User]] = Future {
//    db.withSession { implicit session =>
//      val q = users.where(r => r.id === id && r.userId === user.id)
//      q.map(_.name).update(patch.name)
//      q.firstOption.map(_.toUser)
//    }
//  }
//
//  def delete(id: Int)(implicit user: User): Future[Unit] = Future {
//    db.withSession { implicit session =>
//      users.where(r => r.id === id && r.userId === user.id).delete
//    }
//  }
}
