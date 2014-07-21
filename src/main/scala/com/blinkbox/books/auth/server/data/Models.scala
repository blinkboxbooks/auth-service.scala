package com.blinkbox.books.auth.server.data

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.RefreshTokenStatus
import java.util.concurrent.TimeUnit
import com.blinkbox.books.time.Clock
import org.joda.time.DateTime
import scala.concurrent.duration.FiniteDuration

case class User(id: Int, createdAt: DateTime, updatedAt: DateTime, username: String, firstName: String, lastName: String, passwordHash: String, allowMarketing: Boolean)

case class Client(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean)

case class RefreshToken(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, clientId: Option[Int], token: String, isRevoked: Boolean, expiresAt: DateTime, elevationExpiresAt: DateTime, criticalElevationExpiresAt: DateTime) {
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
