package com.blinkbox.books.auth.server.data

import java.util.concurrent.TimeUnit

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.RefreshTokenStatus
import com.blinkbox.books.time.Clock
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

case class UserId(value: Int) extends AnyVal {
  def external = s"urn:blinkbox:zuul:user:$value"
}

object UserId { val Invalid = UserId(-1) }

object ExternalUserId {
  val expr = """urn:blinkbox:zuul:user:(\d+)""".r
  def unapply(idString: String): Option[UserId] = idString match {
    case expr(id) => Some(UserId(id.toInt))
    case _ => None
  }
}

case class ClientId(value: Int) extends AnyVal {
  def external = s"urn:blinkbox:zuul:client:$value"
}

object ClientId { val Invalid = ClientId(-1) }

object ExternalClientId {
  val expr = """urn:blinkbox:zuul:client:(\d+)""".r
  def unapply(idString: String): Option[ClientId] = idString match {
    case expr(id) => Some(ClientId(id.toInt))
    case _ => None
  }
}

case class RefreshTokenId(value: Int) extends AnyVal

object RefreshTokenId { val Invalid = RefreshTokenId(-1) }

case class User(id: UserId,
    createdAt: DateTime,
    updatedAt: DateTime,
    username: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    allowMarketing: Boolean,
    ssoLinked: Boolean = false)

case class Client(id: ClientId, createdAt: DateTime, updatedAt: DateTime, userId: UserId, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean)

case class RefreshToken(id: RefreshTokenId, createdAt: DateTime, updatedAt: DateTime, userId: UserId, clientId: Option[ClientId], token: String, ssoToken: String, isRevoked: Boolean, expiresAt: DateTime, elevationExpiresAt: DateTime, criticalElevationExpiresAt: DateTime) {
  def isExpired(implicit clock: Clock) = expiresAt.isBefore(clock.now())
  def isValid(implicit clock: Clock) = !isExpired && !isRevoked
  def status(implicit clock: Clock) = if (isValid) RefreshTokenStatus.Valid else RefreshTokenStatus.Invalid
  def isElevated(implicit clock: Clock) = !elevationExpiresAt.isBefore(clock.now())
  def isCriticallyElevated(implicit clock: Clock) = !criticalElevationExpiresAt.isBefore(clock.now())
  def elevation(implicit clock: Clock) =
    if (isCriticallyElevated) Elevation.Critical
    else if (isElevated) Elevation.Elevated
    else Elevation.Unelevated
  def elevationDropsAt(implicit clock: Clock) = if (isCriticallyElevated) criticalElevationExpiresAt else elevationExpiresAt
  def elevationDropsIn(implicit clock: Clock) = FiniteDuration(elevationDropsAt.getMillis - clock.now().getMillis, TimeUnit.MILLISECONDS)
}

case class LoginAttempt(createdAt: DateTime, username: String, successful: Boolean, clientIP: String)
