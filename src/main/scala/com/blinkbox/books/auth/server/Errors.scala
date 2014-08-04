package com.blinkbox.books.auth.server

sealed trait ZuulErrorCode
sealed trait ZuulRequestErrorCode extends ZuulErrorCode
sealed trait ZuulRequestErrorReason extends ZuulErrorCode

sealed trait ZuulErrorReason
sealed trait ZuulAuthorizationErrorCode extends ZuulErrorReason
sealed trait ZuulAuthorizationErrorReason extends ZuulErrorReason

trait EnumContainer[T] {
  val reprs: Map[T, String]

  lazy val inverseReprs = reprs.map(_.swap).toMap

  def fromString(s: String): T = inverseReprs(s)
  def toString(c: T): String = reprs(c)
}

object ZuulRequestErrorCode extends EnumContainer[ZuulRequestErrorCode] {
  case object InvalidClient extends ZuulRequestErrorCode
  case object InvalidGrant extends ZuulRequestErrorCode
  case object InvalidRequest extends ZuulRequestErrorCode

  val reprs: Map[ZuulRequestErrorCode, String] = Map(
    InvalidClient -> "invalid_client",
    InvalidGrant -> "invalid_grant",
    InvalidRequest -> "invalid_request"
  )
}

object ZuulRequestErrorReason extends EnumContainer[ZuulRequestErrorReason] {
  case object CountryGeoBlocked extends ZuulRequestErrorReason
  case object UsernameAlreadyTaken extends ZuulRequestErrorReason
  case object ClientLimitReached extends ZuulRequestErrorReason

  val reprs: Map[ZuulRequestErrorReason, String] = Map(
    CountryGeoBlocked -> "country_geoblocked",
    UsernameAlreadyTaken -> "username_already_taken",
    ClientLimitReached -> "client_limit_reached"
  )
}

object ZuulAuthorizationErrorCode extends EnumContainer[ZuulAuthorizationErrorCode] {
  case object InvalidToken extends ZuulAuthorizationErrorCode

  val reprs: Map[ZuulAuthorizationErrorCode, String] = Map(InvalidToken -> "invalid_token")
}

object ZuulAuthorizationErrorReason extends EnumContainer[ZuulAuthorizationErrorReason] {
  case object UnverifiedIdentity extends ZuulAuthorizationErrorReason

  val reprs: Map[ZuulAuthorizationErrorReason, String] = Map(UnverifiedIdentity -> "unverified_identity")
}

sealed trait ZuulException extends Throwable {
  def message: String
}

case class ZuulRequestException(
  message: String, code: ZuulRequestErrorCode, reason: Option[ZuulRequestErrorReason] = None) extends ZuulException

case class ZuulAuthorizationException(
  message: String, code: ZuulAuthorizationErrorCode, reason: Option[ZuulAuthorizationErrorReason] = None) extends ZuulException

object FailWith {
  import com.blinkbox.books.auth.server.ZuulAuthorizationErrorCode._
  import com.blinkbox.books.auth.server.ZuulAuthorizationErrorReason._
  import com.blinkbox.books.auth.server.ZuulRequestErrorCode._
  import com.blinkbox.books.auth.server.ZuulRequestErrorReason._

  def invalidRefreshToken = throw ZuulRequestException("The refresh token is invalid.", InvalidGrant)
  def refreshTokenNotAuthorized = throw ZuulRequestException("Your client is not authorised to use this refresh token", InvalidClient)
  def unverifiedIdentity = throw ZuulAuthorizationException("Access token is invalid", InvalidToken, Some(UnverifiedIdentity))
  def termsAndConditionsNotAccepted = throw ZuulRequestException("You must accept the terms and conditions", InvalidRequest)
  def passwordTooShort = throw ZuulRequestException("Password must be at least 6 characters", InvalidRequest)
  def notInTheUK = throw ZuulRequestException("You must be in the UK to register", InvalidRequest, Some(CountryGeoBlocked))
  def invalidUsernamePassword = throw ZuulRequestException("The username and/or password is incorrect.", InvalidGrant)
  def invalidClientCredentials = throw ZuulRequestException("Invalid client credentials.", InvalidClient)
  def clientLimitReached = throw ZuulRequestException("Max clients ($MaxClients) already registered", InvalidRequest, Some(ClientLimitReached))

  def requestException(message: String, code: ZuulRequestErrorCode, reason: Option[ZuulRequestErrorReason] = None) =
    throw ZuulRequestException(message, code, reason)
}
