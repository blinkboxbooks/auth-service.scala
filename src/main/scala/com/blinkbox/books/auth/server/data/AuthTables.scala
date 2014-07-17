package com.blinkbox.books.auth.server.data

import com.blinkbox.books.slick.JdbcSupport
import org.joda.time.DateTime

trait AuthTables {
  this: JdbcSupport =>

  import driver.simple._

  lazy val users = TableQuery[Users]
  lazy val clients = TableQuery[Clients]
  lazy val refreshTokens = TableQuery[RefreshTokens]
  lazy val loginAttempts = TableQuery[LoginAttempts]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def firstName = column[String]("first_name", O.NotNull)
    def lastName = column[String]("last_name", O.NotNull)
    def passwordHash = column[String]("password_hash", O.NotNull)
    def allowMarketing = column[Boolean]("allow_marketing_communications", O.NotNull)
    def * = (id, createdAt, updatedAt, username, firstName, lastName, passwordHash, allowMarketing) <> (User.tupled, User.unapply)
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
    def * = (id, createdAt, updatedAt, userId, name, brand, model, os, secret, isDeregistered) <> (Client.tupled, Client.unapply)
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
    def * = (id, createdAt, updatedAt, userId, clientId, token, isRevoked, expiresAt, elevationExpiresAt, criticalElevationExpiresAt) <> (RefreshToken.tupled, RefreshToken.unapply)
    def indexOnToken = index("index_refresh_tokens_on_token", token)
    def user = foreignKey("fk_refresh_tokens_to_users", userId, users)(_.id)
  }

  class LoginAttempts(tag: Tag) extends Table[LoginAttempt](tag, "login_attempts") {
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def successful = column[Boolean]("successful", O.NotNull)
    def clientIP = column[String]("client_ip", O.NotNull)
    def * = (createdAt, username, successful, clientIP) <> (LoginAttempt.tupled, LoginAttempt.unapply)
  }
}
