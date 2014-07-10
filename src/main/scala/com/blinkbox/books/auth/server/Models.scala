package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.Elevation.Elevation
import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field
import org.joda.time.DateTime


object OAuthServerErrorCode extends Enumeration {
  type OAuthServerErrorCode = Value
  val InvalidClient = Value("invalid_client")
  val InvalidGrant = Value("invalid_grant")
  val InvalidRequest = Value("invalid_request")
}

object OAuthServerErrorReason extends Enumeration {
  type OAuthServerErrorReason = Value
  val UsernameAlreadyTaken = Value("username_already_taken")
  val ClientLimitReached = Value("client_limit_reached")
}

import OAuthServerErrorCode._
import OAuthServerErrorReason._

class OAuthServerException(message: String, val code: OAuthServerErrorCode, val reason: Option[OAuthServerErrorReason] = None) extends Exception(message)
class UserAlreadyExists(message: String) extends OAuthServerException(message, InvalidRequest, Some(UsernameAlreadyTaken))

object OAuthClientErrorCode extends Enumeration {
  type OAuthClientErrorCode = Value
  val InvalidToken = Value("invalid_token")
}

object OAuthClientErrorReason extends Enumeration {
  type OAuthClientErrorReason = Value
  val UnverifiedIdentity = Value("unverified_identity")
}

import OAuthClientErrorCode._
import OAuthClientErrorReason._

class OAuthClientException(message: String, val code: OAuthClientErrorCode, val reason: Option[OAuthClientErrorReason] = None) extends Exception(message)

object RefreshTokenStatus extends Enumeration {
  type RefreshTokenStatus = Value
  val Valid = Value("VALID")
  val Invalid = Value("INVALID")
}

import RefreshTokenStatus._

case class OAuthServerError(
  error: OAuthServerErrorCode,
  error_reason: Option[OAuthServerErrorReason],
  error_description: String)

object OAuthServerError {
  def apply(e: OAuthServerException): OAuthServerError = OAuthServerError(e.code, e.reason, e.getMessage)
}

// TODO: Add user properties
@ApiModel(description = "A user")
case class AuthUser(
  @(ApiModelProperty @field)(position = 0, value = "The globally unique identifier") guid: String,
  @(ApiModelProperty @field)(position = 1, value = "The simple identifier") id: Int,
  @(ApiModelProperty @field)(position = 2, value = "The name") name: String)

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
    val details = List(clientName, clientBrand, clientModel, clientOS).flatten
    details.size match {
      case 4 => Some(ClientRegistration(details(0), details(1), details(2), details(3)))
      case 0 => None
      case _ => throw new OAuthServerException("Incomplete client details", InvalidRequest)
    }
  }
}

case class PasswordCredentials(username: String, password: String, clientId: Option[String], clientSecret: Option[String]) {
  if (clientId.isDefined ^ clientSecret.isDefined) throw new OAuthServerException("Both client id and client secret are required.", InvalidClient)
}

case class ClientRegistration(name: String,
                              brand: String,
                              model: String,
                              os: String)

@ApiModel(description = "A user")
case class TokenInfo(
  access_token: String,
token_type: String,
  expires_in: Int,
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

case class SessionInfo(
  token_status: RefreshTokenStatus,
  token_elevation: Option[Elevation],
  token_elevation_expires_in: Option[Long],
  user_roles: Option[List[String]] = None
                        )

// TODO: Add user patch properties
@ApiModel(description = "Updates to a user")
case class UserPatch(
  @(ApiModelProperty @field)(position = 0, value = "The name") name: String) {
  require(name.length > 0)
}
