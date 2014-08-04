package com.blinkbox.books.auth.server.data

import java.security.SecureRandom

import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.slick.SlickSupport
import com.blinkbox.books.time.{Clock, TimeSupport}
import com.blinkbox.security.jwt.util.Base64
import spray.http.RemoteAddress

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait AuthRepository[Profile <: BasicProfile] extends SlickSupport[Profile] {
  def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit
  def authenticateClient(id: String, secret: String, userId: UserId)(implicit session: Session): Option[Client]
  def createRefreshToken(userId: UserId, clientId: Option[ClientId])(implicit session: Session): RefreshToken
  def refreshTokenWithId(id: RefreshTokenId)(implicit session: Session): Option[RefreshToken]
  def refreshTokenWithToken(token: String)(implicit session: Session): Option[RefreshToken]
  def refreshTokensByClientId(clientId: ClientId)(implicit session: Session): List[RefreshToken]
  def associateRefreshTokenWithClient(t: RefreshToken, c: Client)(implicit session: Session)
  def extendRefreshTokenLifetime(t: RefreshToken)(implicit session: Session): Unit
  def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit
}

trait JdbcAuthRepository extends AuthRepository[JdbcProfile] with ZuulTables {
  this: TimeSupport =>
  import driver.simple._

  val ClientIdExpr = """urn:blinkbox:zuul:client:([0-9]+)""".r

  override def recordLoginAttempt(username: String, succeeded: Boolean, clientIP: Option[RemoteAddress])(implicit session: Session): Unit = {
    loginAttempts += LoginAttempt(clock.now(), username, succeeded, clientIP.fold("unknown")(_.toString()))
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

  override def revokeRefreshToken(t: RefreshToken)(implicit session: Session): Unit =
    refreshTokens.where(_.id === t.id).map(_.isRevoked).update(true)

  private def newRefreshToken(userId: UserId, clientId: Option[ClientId]) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val token = Base64.encode(buf)
    RefreshToken(RefreshTokenId.Invalid, now, now, userId, clientId, token, false, now.plusDays(90), now.plusHours(24), now.plusMinutes(10))
  }
}

class DefaultAuthRepository(val tables: ZuulTables)(implicit val clock: Clock)
  extends TimeSupport with ZuulTablesSupport with JdbcAuthRepository
