package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.EnumContainer
import org.joda.time.DateTime

import scala.concurrent.duration._

sealed trait SsoToken extends Any {
  def value: String
}
case class SsoAccessToken(value: String) extends AnyVal with SsoToken
case class SsoPasswordResetToken(value: String) extends AnyVal with SsoToken
case class SsoRefreshToken(value: String) extends AnyVal with SsoToken

case class SsoUserId(value: String) extends AnyVal

case class SsoCredentials(accessToken: SsoAccessToken, tokenType: String, expiresIn: Int, refreshToken: SsoRefreshToken) {
  require(tokenType.toLowerCase == "bearer", s"Unrecognized token type: $tokenType")
}

case class SsoUserCredentials(userId: SsoUserId, credentials: SsoCredentials)

case class SsoPasswordResetTokenResponse(resetToken: SsoPasswordResetToken, expiresIn: Long) {
  val expiresInDuration: FiniteDuration = expiresIn.seconds
}

sealed trait SsoTokenStatus

object SsoTokenStatus extends EnumContainer[SsoTokenStatus] {
  case object Invalid extends SsoTokenStatus
  case object Expired extends SsoTokenStatus
  case object Revoked extends SsoTokenStatus
  case object Valid extends SsoTokenStatus

  override val reprs: Map[SsoTokenStatus, String] = Map(
    Invalid -> "invalid",
    Expired -> "expired",
    Revoked -> "revoked",
    Valid -> "valid"
  )
}

sealed trait SsoTokenElevation

object SsoTokenElevation extends EnumContainer[SsoTokenElevation] {
  case object None extends SsoTokenElevation
  case object Critical extends SsoTokenElevation

  override val reprs: Map[SsoTokenElevation, String] = Map(
    None -> "none",
    Critical -> "critical"
  )
}

case class TokenStatus(
  status: SsoTokenStatus,
  issuedAt: Option[DateTime],
  expiresAt: Option[DateTime],
  tokenType: Option[String],
  sessionElevation: Option[SsoTokenElevation],
  sessionElevationExpiresIn: Option[Long])

case class LinkedAccount(service: String, serviceUserId: String, serviceAllowMarketing: Boolean)

case class UserInformation(userId: SsoUserId, username: String, firstName: String, lastName: String, linkedAccounts: List[LinkedAccount])
