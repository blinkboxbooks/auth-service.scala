package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId

import scala.concurrent.Future

sealed trait Token extends Request
case class RefreshToken() extends Token
case class PasswordResetToken() extends Token
case class AccessToken() extends Token

// These traits has more a documenting function than a functional one
sealed trait Request
sealed trait AdminRequest extends Request
sealed trait Response
sealed trait AdminResponse extends Response

case class RegisterUser(
  id: UserId,
  firstName: String,
  lastName: String,
  username: String,
  password: String,
  acceptedTermsVersion: String,
  allowMarketing: Boolean) extends Request

// case class AuthenticateUser() extends Request
// case class RevokeToken(token: Token)
// case class LinkAccount() extends Request
// case class GeneratePasswordReset() extends Request
// case class UpdatePassword() extends Request
// case class GetTokenStatus() extends Request
// case class PatchUser() extends Request

// case class SearchUser() extends AdminRequest
// case class GetUserDetails() extends AdminRequest
// case class UpdateUser() extends AdminRequest

case class TokenCredentials(accessToken: String, tokenType: String, expiresIn: Int, refreshToken: String) extends Response {
  require(tokenType == "bearer", s"Unrecognized token type: $tokenType")
}
// case class PasswordResetCredentials() extends Response
// case class TokenStatus() extends Response
// case class UserInformation() extends Response
// case class SystemStatus() extends Response

// case class SearchUserResult() extends AdminResponse
// case class UserDetail() extends AdminResponse
