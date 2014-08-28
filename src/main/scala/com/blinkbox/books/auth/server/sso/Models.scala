package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.EnumContainer
import org.joda.time.DateTime

import scala.concurrent.duration._

sealed trait SSOToken extends Any {
  def value: String
}
case class SSOAccessToken(value: String) extends AnyVal with SSOToken
case class SSOPasswordResetToken(value: String) extends AnyVal with SSOToken
case class SSORefreshToken(value: String) extends AnyVal with SSOToken

case class SSOUserId(value: String) extends AnyVal

case class SSOCredentials(accessToken: SSOAccessToken, tokenType: String, expiresIn: Int, refreshToken: SSORefreshToken) {
  require(tokenType.toLowerCase == "bearer", s"Unrecognized token type: $tokenType")
}

case class SSOUserCredentials(userId: SSOUserId, credentials: SSOCredentials)

case class SSOPasswordResetTokenResponse(resetToken: SSOPasswordResetToken, expiresIn: Long) {
  val expiresInDuration: FiniteDuration = expiresIn.seconds
}

sealed trait SSOTokenStatus

object SSOTokenStatus extends EnumContainer[SSOTokenStatus] {
  case object Invalid extends SSOTokenStatus
  case object Expired extends SSOTokenStatus
  case object Revoked extends SSOTokenStatus
  case object Valid extends SSOTokenStatus

  override val reprs: Map[SSOTokenStatus, String] = Map(
    Invalid -> "invalid",
    Expired -> "expired",
    Revoked -> "revoked",
    Valid -> "valid"
  )
}

sealed trait SSOTokenElevation

object SSOTokenElevation extends EnumContainer[SSOTokenElevation] {
  case object None extends SSOTokenElevation
  case object Critical extends SSOTokenElevation

  override val reprs: Map[SSOTokenElevation, String] = Map(
    None -> "none",
    Critical -> "critical"
  )
}

case class TokenStatus(
  status: SSOTokenStatus,
  issuedAt: DateTime,
  expiresAt: DateTime,
  tokenType: String,
  sessionElevation: SSOTokenElevation,
  sessionElevationExpiresIn: Int)

case class LinkedAccount(service: String, serviceUserId: String, serviceAllowMarketing: Boolean)

case class UserInformation(userId: SSOUserId, username: String, firstName: String, lastName: String, linkedAccounts: List[LinkedAccount])
