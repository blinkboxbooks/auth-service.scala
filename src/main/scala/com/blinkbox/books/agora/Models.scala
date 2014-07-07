package com.blinkbox.books.agora

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field
import org.joda.time.DateTime


case class OAuthError(
  error: String,
  error_description: String,
error_reason: Option[String] = None
                       )

// TODO: Add user properties
@ApiModel(description = "A user")
case class AuthUser(
  @(ApiModelProperty @field)(position = 0, value = "The globally unique identifier") guid: String,
  @(ApiModelProperty @field)(position = 1, value = "The simple identifier") id: Int,
  @(ApiModelProperty @field)(position = 2, value = "The name") name: String)

case class Registration(
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
  // TODO: require(name.length > 0)
}


//"client_id" => "urn:blinkbox:zuul:client:#{id}",
//"client_uri" => "/clients/#{id}",
//"client_name" => name,
//"client_brand" => brand,
//"client_model" => model,
//"client_os" => os,
//"last_used_date" => updated_at.utc.strftime("%F")

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

// TODO: Add user patch properties
@ApiModel(description = "Updates to a user")
case class UserPatch(
  @(ApiModelProperty @field)(position = 0, value = "The name") name: String) {
  require(name.length > 0)
}
