package com.blinkbox.books.auth.server

import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.spray._
import com.mysql.jdbc.MysqlDataTruncation
import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver.simple._
//import java.sql.Date
import com.lambdaworks.crypto.SCryptUtil
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import java.security.SecureRandom
import com.blinkbox.security.jwt.util.Base64
import scala.Some
import org.joda.time.{DateTimeZone, DateTime}


// TODO: Use a proper database

trait UserService {
  def registerUser(registration: UserRegistration): Future[TokenInfo]
//  def list(page: Page)(implicit user: User): Future[ListPage[User]]
//  def getById(id: Int)(implicit user: User): Future[Option[User]]
//  def update(id: Int, patch: UserPatch)(implicit user: User): Future[Option[User]]
//  def delete(id: Int)(implicit user: User): Future[Unit]
}

case class User(id: Int, createdAt: DateTime, updatedAt: DateTime, username: String, firstName: String, lastName: String, passwordHash: String, allowMarketing: Boolean)
object User {
  import scala.language.implicitConversions
  def newUser(r: UserRegistration) = {
    val now = DateTime.now(DateTimeZone.UTC)
    val h = SCryptUtil.scrypt(r.password, 16384, 8, 1)
    User(-1, now, now, r.username, r.firstName, r.lastName, h, r.allowMarketing)
  }
}

case class Client(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean)
object Client {
  def newClient(userId: Int, r: ClientRegistration) = {
    val now = DateTime.now(DateTimeZone.UTC)
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val secret = Base64.encode(buf)
    Client(-1, now, now, userId, r.name, r.brand, r.model, r.os, secret, false)
  }
}

case class RefreshToken(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, clientId: Option[Int], token: String, isRevoked: Boolean, expiresAt: DateTime, elevationExpiresAt: DateTime, criticalElevationExpiresAt: DateTime)
object RefreshToken {
  def newToken(userId: Int) = {
    val now = DateTime.now(DateTimeZone.UTC)
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val token = Base64.encode(buf)
    RefreshToken(-1, now, now, userId, None /* TODO */, token, false, now.plusDays(90), now.plusHours(24), now.plusMinutes(10))
  }
}

object Tables {

  implicit def dateTime = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime))

  val users = TableQuery[Users]
  val clients = TableQuery[Clients]
  val refreshTokens = TableQuery[RefreshTokens]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def firstName = column[String]("first_name", O.NotNull)
    def lastName = column[String]("last_name", O.NotNull)
    def passwordHash = column[String]("password_hash", O.NotNull)
    def allowMarketing = column[Boolean]("allow_marketing_communications", O.NotNull)
    def * = (id, createdAt, updatedAt, username, firstName, lastName, passwordHash, allowMarketing) <> ((User.apply _).tupled, User.unapply)
    def indexOnUsername = index("index_users_on_username", username, unique = true)
  }

  class Clients(tag: Tag) extends Table[Client](tag, "clients") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def name = column[String]("name", O.NotNull)
    def brand = column[String]("brand", O.NotNull)
    def model = column[String]("model", O.NotNull)
    def os = column[String]("os", O.NotNull)
    def secret = column[String]("client_secret", O.NotNull)
    def isDeregistered = column[Boolean]("deregistered", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, name, brand, model, os, secret, isDeregistered) <> ((Client.apply _).tupled, Client.unapply)
    def indexOnUserId = index("index_users_on_username", userId)
  }

  class RefreshTokens(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def clientId = column[Option[Int]]("client_id")
    def token = column[String]("token", O.NotNull)
    def isRevoked = column[Boolean]("revoked", O.NotNull)
    def expiresAt = column[DateTime]("expires_at", O.NotNull)
    def elevationExpiresAt = column[DateTime]("elevation_expires_at", O.NotNull)
    def criticalElevationExpiresAt = column[DateTime]("critical_elevation_expires_at", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, clientId, token, isRevoked, expiresAt, elevationExpiresAt, criticalElevationExpiresAt) <> ((RefreshToken.apply _).tupled, RefreshToken.unapply)
    def indexOnToken = index("index_refresh_tokens_on_token", token)
    def user = foreignKey("fk_refresh_tokens_to_users", userId, users)(_.id)
  }
}

import OAuthErrorCode._
import OAuthErrorReason._

class DefaultUserService(config: DatabaseConfig)(implicit executionContext: ExecutionContext) extends UserService {
  val db = Database.forURL("jdbc:" + config.uri.withUserInfo("").toString, driver="com.mysql.jdbc.Driver", user = "zuul", password = "mypass")

  import Tables._

  def registerUser(registration: UserRegistration): Future[TokenInfo] = Future {
    // TODO: Just use Future.failed rather than throwing here
    if (!registration.acceptedTerms) throw new OAuthException("You must accept the terms and conditions.", InvalidRequest)
    if (registration.password.length < 6) throw new OAuthException("password must be at least 6 characters", InvalidRequest)

    val (user, client, token) = db.withTransaction { implicit transaction =>
      val userId = (users returning users.map(_.id)) += User.newUser(registration)
      val c = registration.client.map { cr =>
        val clientId = (clients returning clients.map(_.id)) += Client.newClient(userId, cr)
        clients.where(_.id === clientId).first
      }      
      val rtId = (refreshTokens returning refreshTokens.map(_.id)) += RefreshToken.newToken(userId) // TODO: ClientId

      val q = for {
        rt <- refreshTokens.where(_.id === rtId)
        u  <- rt.user
      } yield (u, rt)

      val (u, t) :: Nil = q.list
      (u, c, t)
    }
    TokenInfo(
      access_token = "the access token",
      token_type = "bearer",
      expires_in = 1800,
      refresh_token = Some(token.token),
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
      client_secret = client.map(_.secret),
      last_used_date = client.map(_.updatedAt))
  } recoverWith {
    case e: MysqlDataTruncation => Future.failed(new OAuthException(e.getMessage, InvalidRequest))
    case e: MySQLIntegrityConstraintViolationException => Future.failed(new UserAlreadyExists(e.getMessage))
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
