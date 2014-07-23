package com.blinkbox.books.auth.server.data

import java.security.SecureRandom

import com.blinkbox.books.auth.server.{ClientRegistration, UserRegistration}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.slick.{JdbcSupport, SlickSupport}
import com.blinkbox.books.time.{TimeSupport, Clock}
import com.blinkbox.security.jwt.util.Base64
import com.lambdaworks.crypto.SCryptUtil
import spray.http.RemoteAddress

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait AuthRepository[Profile <: BasicProfile] extends SlickSupport[Profile] {
  def updateUser(user: User)(implicit session: Session): Unit
  def createUser(registration: UserRegistration)(implicit session: Session): User
  def authenticateUser(username: String, password: String)(implicit session: Session): Option[User]
  def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit
  def userWithId(id: Int)(implicit session: Session): Option[User]
  def createClient(userId: Int, registration: ClientRegistration)(implicit session: Session): Client
  def authenticateClient(id: String, secret: String, userId: Int)(implicit session: Session): Option[Client]
  def activeClients(implicit session: Session, user: AuthenticatedUser): List[Client]
  def activeClientCount(implicit session: Session, user: AuthenticatedUser): Int
  def clientWithId(id: Int)(implicit session: Session, user: AuthenticatedUser): Option[Client]
  def updateClient(client: Client)(implicit session: Session, user: AuthenticatedUser): Unit
  def createRefreshToken(userId: Int, clientId: Option[Int])(implicit session: Session): RefreshToken
  def refreshTokenWithId(id: Int)(implicit session: Session): Option[RefreshToken]
  def refreshTokenWithToken(token: String)(implicit session: Session): Option[RefreshToken]
  def refreshTokensByClientId(clientId: Int)(implicit session: Session): List[RefreshToken]
  def associateRefreshTokenWithClient(t: RefreshToken, c: Client)(implicit session: Session)
  def extendRefreshTokenLifetime(t: RefreshToken)(implicit session: Session): Unit
  def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit
}

trait JdbcAuthRepository extends AuthRepository[JdbcProfile] with AuthTables {
  this: JdbcSupport with TimeSupport =>
  import driver.simple._

  val ClientId = """urn:blinkbox:zuul:client:([0-9]+)""".r

  def createUser(registration: UserRegistration)(implicit session: Session): User = {
    val user = newUser(registration)
    val id = (users returning users.map(_.id)) += user
    user.copy(id = id)
  }

  def authenticateUser(username: String, password: String)(implicit session: Session): Option[User] = {
    val user = users.where(_.username === username).firstOption
    val passwordValid = if (user.isDefined) {
      SCryptUtil.check(password, user.get.passwordHash)
    } else {
      // even if the user isn't found we still need to perform an scrypt hash of something to help
      // prevent timing attacks as this hashing process is the bulk of the request time
      hashPassword("random string")
      false
    }
    if (passwordValid) user else None
  }

  def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit = {
    loginAttempts += LoginAttempt(clock.now(), username, succeeded, clientIP.fold("unknown")(_.toString()))
  }

  def createClient(userId: Int, registration: ClientRegistration)(implicit session: Session): Client = {
    val client = newClient(userId, registration)
    val id = (clients returning clients.map(_.id)) += client
    client.copy(id = id)
  }

  def authenticateClient(id: String, secret: String, userId: Int)(implicit session: Session): Option[Client] = {
    if (id.isEmpty || secret.isEmpty) return None

    val numericId = id match {
      case ClientId(n) => try Some(n.toInt) catch { case _: NumberFormatException => None }
      case _ => None
    }

    val client = numericId.flatMap(nid => clients.where(c => c.id === nid && c.secret === secret && c.userId === userId && c.isDeregistered === false).firstOption)
    if (client.isDefined) {
      clients.where(_.id === client.get.id).map(_.updatedAt).update(clock.now())
    }

    client
  }

  def createRefreshToken(userId: Int, clientId: Option[Int])(implicit session: Session): RefreshToken = {
    val token = newRefreshToken(userId, clientId)
    val id = (refreshTokens returning refreshTokens.map(_.id)) += token
    token.copy(id = id)
  }

  def userWithId(id: Int)(implicit session: Session) = users.where(_.id === id).list.headOption


  def refreshTokenWithId(id: Int)(implicit session: Session) = {
    val refreshToken = refreshTokens.where(rt => rt.id === id && !rt.isRevoked).list.headOption
    refreshToken.filter(_.isValid)
  }

  def refreshTokenWithToken(token: String)(implicit session: Session) = {
    val refreshToken = refreshTokens.where(rt => rt.token === token && !rt.isRevoked).list.headOption
    refreshToken.filter(_.isValid)
  }

  def refreshTokensByClientId(clientId: Int)(implicit session: Session): List[RefreshToken] = {
    refreshTokens.where(_.clientId === clientId).list
  }

  def associateRefreshTokenWithClient(t: RefreshToken, c: Client)(implicit session: Session) = {
    refreshTokens.where(_.id === t.id).map(t => (t.updatedAt, t.clientId)).update(clock.now(), Some(c.id))
  }

  def extendRefreshTokenLifetime(t: RefreshToken)(implicit session: Session) = {
    // TODO: Make lifetime extension configurable
    val now = clock.now()
    refreshTokens.where(_.id === t.id).map(t => (t.updatedAt, t.expiresAt)).update(now, now.plusDays(90))
  }

  def activeClients(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.userId === user.id && !c.isDeregistered).list
  }

  def activeClientCount(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.userId === user.id && !c.isDeregistered).length.run
  }

  def clientWithId(id: Int)(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.id === id && c.userId === user.id && !c.isDeregistered).list.headOption
  }
  
  def updateClient(client: Client)(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.id === client.id && c.userId === user.id).update(client)
  }

  def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit =
    refreshTokens.where(_.id === t.id).map(_.isRevoked).update(true)

  override def updateUser(user: User)(implicit session: Session): Unit =
    users.where(_.id === user.id).update(user)

  private def newUser(r: UserRegistration) = {
    val now = clock.now()
    val passwordHash = hashPassword(r.password)
    User(-1, now, now, r.username, r.firstName, r.lastName, passwordHash, r.allowMarketing)
  }

  private def newClient(userId: Int, r: ClientRegistration) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val secret = Base64.encode(buf)
    Client(-1, now, now, userId, r.name, r.brand, r.model, r.os, secret, false)
  }

  private def newRefreshToken(userId: Int, clientId: Option[Int]) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val token = Base64.encode(buf)
    RefreshToken(-1, now, now, userId, clientId, token, false, now.plusDays(90), now.plusHours(24), now.plusMinutes(10))
  }

  private def hashPassword(password: String) = SCryptUtil.scrypt(password, 16384, 8, 1)
}

class DefaultAuthRepository(val driver: JdbcProfile)(implicit val clock: Clock)
  extends TimeSupport with JdbcSupport with JdbcAuthRepository
