package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.Elevation.Elevation
import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidClient
import com.blinkbox.books.auth.server.sso.{SsoPasswordResetToken, SsoTokenStatus}
import com.blinkbox.books.auth.server.sso.SsoTokenStatus.{Revoked, Expired}
import org.joda.time.{DateTime, LocalDate}

object TokenStatus extends Enumeration {
  type TokenStatus = Value
  val Valid = Value("VALID")
  val Invalid = Value("INVALID")

  def fromSSOValidity(v: SsoTokenStatus) = v match {
    case SsoTokenStatus.Invalid | Expired | Revoked => Invalid
    case SsoTokenStatus.Valid => Valid
  }
}

import com.blinkbox.books.auth.server.TokenStatus._

case class UserRegistration(
  firstName: String,
  lastName: String,
  username: String,
  password: String,
  acceptedTerms: Boolean,
  allowMarketing: Boolean,
  clientName: Option[String],
  clientBrand: Option[String],
  clientModel: Option[String],
  clientOS: Option[String]) {
  val client = {
    val details = Seq(clientName, clientBrand, clientModel, clientOS).flatten

    require(details.size == 0 || details.size == 4, "Incomplete client details")

    details.size match {
      case 4 => Some(ClientRegistration(details(0), details(1), details(2), details(3)))
      case 0 => None
    }
  }
}

trait ClientCredentials {
  val clientId: Option[String]
  val clientSecret: Option[String]

  if (clientId.isDefined != clientSecret.isDefined) throw Failures.invalidClientCredentials

  val asPair: Option[(String, String)] = for (id <- clientId; secret <- clientSecret) yield (id, secret)
}

case class PasswordCredentials(username: String, password: String, clientId: Option[String], clientSecret: Option[String]) extends ClientCredentials {
  if (clientId.isDefined ^ clientSecret.isDefined) throw Failures.requestException("Both client id and client secret are required.", InvalidClient)
}

case class RefreshTokenCredentials(token: String, clientId: Option[String], clientSecret: Option[String]) extends ClientCredentials {
  if (clientId.isDefined ^ clientSecret.isDefined) throw Failures.requestException("Both client id and client secret are required.", InvalidClient)
}

case class ResetTokenCredentials(resetToken: SsoPasswordResetToken, newPassword: String, clientId: Option[String], clientSecret: Option[String]) extends ClientCredentials {
  if (clientId.isDefined ^ clientSecret.isDefined) throw Failures.requestException("Both client id and client secret are required.", InvalidClient)
}

case class ClientRegistration(
  name: String,
  brand: String,
  model: String,
  os: String) {

  require(!name.isEmpty, "Name must not be empty if provided")
  require(!brand.isEmpty, "Brand must not be empty if provided")
  require(!model.isEmpty, "Model must not be empty if provided")
  require(!os.isEmpty, "OS must not be empty if provided")
}

case class UserInfo(
  user_id: String,
  user_uri: String,
  user_username: String,
  user_first_name: String,
  user_last_name: String,
  user_allow_marketing_communications: Boolean
)

case class TokenInfo(
  access_token: String,
  token_type: String,
  expires_in: Long,
  refresh_token: Option[String],
  user_id: String,
  user_uri: String,
  user_username: String,
  user_first_name: String,
  user_last_name: String,
  client_id: Option[String] = None,
  client_uri: Option[String] = None,
  client_name: Option[String] = None,
  client_brand: Option[String] = None,
  client_model: Option[String] = None,
  client_os: Option[String] = None,
  client_secret: Option[String] = None,
  last_used_date: Option[DateTime] = None)

case class ClientInfo(
  client_id: String,
  client_uri: String,
  client_name: String,
  client_brand: String,
  client_model: String,
  client_os: String,
  client_secret: Option[String],
  last_used_date: LocalDate)

case class ClientPatch(
  client_name: Option[String] = None,
  client_brand: Option[String] = None,
  client_model: Option[String] = None,
  client_os: Option[String] = None) {

  require(client_brand.isDefined || client_name.isDefined || client_model.isDefined || client_os.isDefined,
    "Invalid client update (no information provided)")
}

case class ClientList(clients: List[ClientInfo])

case class SessionInfo(
  token_status: TokenStatus,
  token_elevation: Option[Elevation],
  token_elevation_expires_in: Option[Long],
  user_roles: Option[List[String]] = None)

case class UserPatch(
  first_name: Option[String],
  last_name: Option[String],
  username: Option[String],
  allow_marketing_communications: Option[Boolean],
  accepted_terms_and_conditions: Option[Boolean]) {

  require(first_name.isDefined || last_name.isDefined || username.isDefined || allow_marketing_communications.isDefined,
    "Invalid user update (no information provided)")

  require(accepted_terms_and_conditions.getOrElse(true), "Cannot change terms & conditions acceptance")
}
