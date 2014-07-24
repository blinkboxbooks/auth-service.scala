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
  def authenticateUser(username: String, password: String)(implicit session: Session): Option[User]
  def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit
  def createClient(userId: UserId, registration: ClientRegistration)(implicit session: Session): Client
  def authenticateClient(id: String, secret: String, userId: UserId)(implicit session: Session): Option[Client]
  def activeClients(implicit session: Session, user: AuthenticatedUser): List[Client]
  def activeClientCount(implicit session: Session, user: AuthenticatedUser): Int
  def clientWithId(id: ClientId)(implicit session: Session, user: AuthenticatedUser): Option[Client]
  def updateClient(client: Client)(implicit session: Session, user: AuthenticatedUser): Unit
  def createRefreshToken(userId: UserId, clientId: Option[ClientId])(implicit session: Session): RefreshToken
  def refreshTokenWithId(id: RefreshTokenId)(implicit session: Session): Option[RefreshToken]
  def refreshTokenWithToken(token: String)(implicit session: Session): Option[RefreshToken]
  def refreshTokensByClientId(clientId: ClientId)(implicit session: Session): List[RefreshToken]
  def associateRefreshTokenWithClient(t: RefreshToken, c: Client)(implicit session: Session)
  def extendRefreshTokenLifetime(t: RefreshToken)(implicit session: Session): Unit
  def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit
}

trait JdbcAuthRepository extends AuthRepository[JdbcProfile] with ZuulTables {
  this: JdbcSupport with TimeSupport =>
  import driver.simple._

  val ClientIdExpr = """urn:blinkbox:zuul:client:([0-9]+)""".r

  override def authenticateUser(username: String, password: String)(implicit session: Session): Option[User] = {
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

  override def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit = {
    loginAttempts += LoginAttempt(clock.now(), username, succeeded, clientIP.fold("unknown")(_.toString()))
  }

  override def createClient(userId: UserId, registration: ClientRegistration)(implicit session: Session): Client = {
    val client = newClient(userId, registration)
    val id = (clients returning clients.map(_.id)) += client
    client.copy(id = id)
  }

  override def authenticateClient(id: String, secret: String, userId: UserId)(implicit session: Session): Option[Client] = {
    if (id.isEmpty || secret.isEmpty) return None

    val numericId = id match {
      case ClientIdExpr(n) => try Some(ClientId(n.toInt)) catch { case _: NumberFormatException => None }
      case _ => None
    }

    val client = numericId.flatMap(nid => clients.where(c => c.id === nid && c.secret === secret && c.userId === userId && c.isDeregistered === false).firstOption)
    if (client.isDefined) {
      clients.where(_.id === client.get.id).map(_.updatedAt).update(clock.now())
    }

    client
  }

  override def createRefreshToken(userId: UserId, clientId: Option[ClientId])(implicit session: Session): RefreshToken = {
    val token = newRefreshToken(userId, clientId)
    val id = (refreshTokens returning refreshTokens.map(_.id)) += token
    token.copy(id = id)
  }

  override def refreshTokenWithId(id: RefreshTokenId)(implicit session: Session) = {
    val refreshToken = refreshTokens.where(rt => rt.id === id && !rt.isRevoked).list.headOption
    refreshToken.filter(_.isValid)
  }

  override def refreshTokenWithToken(token: String)(implicit session: Session) = {
    val refreshToken = refreshTokens.where(rt => rt.token === token && !rt.isRevoked).list.headOption
    refreshToken.filter(_.isValid)
  }

  override def refreshTokensByClientId(clientId: ClientId)(implicit session: Session): List[RefreshToken] = {
    refreshTokens.where(_.clientId === clientId).list
  }

  override def associateRefreshTokenWithClient(t: RefreshToken, c: Client)(implicit session: Session) = {
    refreshTokens.where(_.id === t.id).map(t => (t.updatedAt, t.clientId)).update(clock.now(), Some(c.id))
  }

  override def extendRefreshTokenLifetime(t: RefreshToken)(implicit session: Session) = {
    // TODO: Make lifetime extension configurable
    val now = clock.now()
    refreshTokens.where(_.id === t.id).map(t => (t.updatedAt, t.expiresAt)).update(now, now.plusDays(90))
  }

  override def activeClients(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.userId === UserId(user.id) && !c.isDeregistered).list
  }

  override def activeClientCount(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.userId === UserId(user.id) && !c.isDeregistered).length.run
  }

  override def clientWithId(id: ClientId)(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.id === id && c.userId === UserId(user.id) && !c.isDeregistered).list.headOption
  }

  override def updateClient(client: Client)(implicit session: Session, user: AuthenticatedUser) = {
    clients.where(c => c.id === client.id && c.userId === UserId(user.id)).update(client)
  }

  override def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit =
    refreshTokens.where(_.id === t.id).map(_.isRevoked).update(true)

  private def newUser(r: UserRegistration) = {
    val now = clock.now()
    val passwordHash = hashPassword(r.password)
    User(UserId(-1), now, now, r.username, r.firstName, r.lastName, passwordHash, r.allowMarketing)
  }

  private def newClient(userId: UserId, r: ClientRegistration) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val secret = Base64.encode(buf)
    Client(ClientId(-1), now, now, userId, r.name, r.brand, r.model, r.os, secret, false)
  }

  private def newRefreshToken(userId: UserId, clientId: Option[ClientId]) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val token = Base64.encode(buf)
    RefreshToken(RefreshTokenId(-1), now, now, userId, clientId, token, false, now.plusDays(90), now.plusHours(24), now.plusMinutes(10))
  }

  private def hashPassword(password: String) = SCryptUtil.scrypt(password, 16384, 8, 1)
}

class DefaultAuthRepository(val tables: ZuulTables)(implicit val clock: Clock)
  extends TimeSupport with ZuulTablesSupport with JdbcAuthRepository
