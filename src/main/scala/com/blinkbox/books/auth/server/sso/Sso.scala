package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.JsonAST.{JField, JObject, JString}
import spray.client.pipelining._
import spray.http.{FormData, HttpCredentials, OAuth2BearerToken, StatusCodes}
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

sealed trait SsoException extends Throwable
case class SsoInvalidAccessToken(receivedCredentials: SsoCredentials) extends SsoException
case object SsoUnauthorized extends SsoException
case object SsoConflict extends SsoException
case object SsoForbidden extends SsoException
case class SsoTooManyRequests(retryAfter: FiniteDuration) extends SsoException
case class SsoInvalidRequest(message: String) extends SsoException
case object SsoNotFound extends SsoException
case class SsoUnknownException(e: Throwable) extends SsoException

object SsoConstants {
  val TokenUri = "/oauth2/token"
  val LinkUri = "/link"
  val UserInfoUri = "/user"
  val RevokeTokenUri = "/tokens/revoke"
  val TokenStatusUri = "/tokens/status"
  val ExtendSessionUri = "/session"
  val UpdatePasswordUri = "/password/update"
  val GeneratePasswordResetTokenUri = "/password/reset/generate-token"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
  val PasswordResetTokenGrant = "urn:blinkbox:oauth:grant-type:password-reset-token"
  val PasswordGrant = "password"
  val RefreshTokenGrant = "refresh_token"
}

trait Sso {
  def register(req: UserRegistration): Future[(SsoUserId, SsoCredentials)]
  def authenticate(c: PasswordCredentials): Future[SsoCredentials]
  def refresh(ssoRefreshToken: SsoRefreshToken): Future[SsoCredentials]
  def resetPassword(passwordToken: SsoPasswordResetToken, newPassword: String): Future[SsoUserCredentials]
  def revokeToken(token: SsoRefreshToken): Future[Unit]
  def linkAccount(token: SsoAccessToken, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit]
  def generatePasswordResetToken(username: String): Future[SsoPasswordResetTokenResponse]
  def updatePassword(token: SsoAccessToken, oldPassword: String, newPassword: String): Future[Unit]
  def sessionStatus(token: SsoAccessToken): Future[TokenStatus]
  def tokenStatus(token: SsoToken): Future[TokenStatus]
  def extendSession(token: SsoAccessToken): Future[Unit]
  def userInfo(token: SsoAccessToken): Future[UserInformation]
  def updateUser(token: SsoAccessToken, req: UserPatch): Future[Unit]
  // Admin
  // def adminSearchUser(req: SearchUser): Future[SearchUserResult]
  // def adminUserDetails(req: GetUserDetails): Future[UserDetail]
  // Health-check
  // def systemStatus(): Future[SystemStatus]
}

class DefaultSso(config: SsoConfig, client: Client, tokenDecoder: SsoAccessTokenDecoder)(implicit ec: ExecutionContext) extends Sso with StrictLogging with FormUnicodeSupport {
  import com.blinkbox.books.auth.server.sso.Serialization.json4sUnmarshaller

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SsoConstants

  private def extractUserId(cred: SsoCredentials): (SsoUserId, SsoCredentials) = SsoDecodedAccessToken.decode(cred.accessToken.value, tokenDecoder) match {
    case Success(token) => (SsoUserId(token.subject), cred)
    case Failure(_) => throw new SsoInvalidAccessToken(cred)
  }

  private def userCredentials(cred: SsoCredentials): SsoUserCredentials = {
    val (id, _) = extractUserId(cred)
    SsoUserCredentials(id, cred)
  }

  private def extractInvalidRequest(e: UnsuccessfulResponseException): SsoException = {
    import org.json4s.jackson.JsonMethods._
    parseOpt(e.response.entity.asString).collect {
      case JObject(
        JField("error", JString("invalid_request")) ::
        JField("error_description", JString(s)) :: Nil) => SsoInvalidRequest(s)
    } getOrElse(SsoUnknownException(e))
  }

  private def extractTooManyRequests(e: UnsuccessfulResponseException): SsoException = try {
    import scala.concurrent.duration._
    e.response.
      headers.
      find(_.lowercaseName == "retry-after").
      map(r => SsoTooManyRequests(r.value.toInt.seconds)).
      getOrElse(SsoUnknownException(e))
  } catch {
    case e: NumberFormatException => SsoUnknownException(e)
  }

  private def commonErrorsTransformer: Throwable => SsoException = {
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Unauthorized => SsoUnauthorized
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Forbidden => SsoForbidden
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Conflict => SsoConflict
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.BadRequest => extractInvalidRequest(e)
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.TooManyRequests => extractTooManyRequests(e)
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.NotFound => SsoNotFound
    case e: Throwable => SsoUnknownException(e)
  }

  // TODO: These transformers should deal with some specific exception and then forward to common for unhandled ones
  private def registrationErrorsTransformer = commonErrorsTransformer
  private def authenticationErrorsTransformer = commonErrorsTransformer
  private def linkErrorsTransformer = commonErrorsTransformer
  private def refreshErrorsTransformer = commonErrorsTransformer
  private def userInfoErrorsTransformer = commonErrorsTransformer
  private def revokeTokenErrorsTransformer = commonErrorsTransformer
  private def tokenStatusErrorsTransformer = commonErrorsTransformer
  private def extendSessionErrorTransformer = commonErrorsTransformer
  private def updateUserErrorTransformer = commonErrorsTransformer
  private def updatePasswordErrorTransformer = commonErrorsTransformer
  private def generatePasswordTokenErrorTransformer = commonErrorsTransformer
  private def resetPasswordErrorTransformer = commonErrorsTransformer

  def oauthCredentials(token: SsoAccessToken): HttpCredentials = new OAuth2BearerToken(token.value)

  def register(req: UserRegistration): Future[(SsoUserId, SsoCredentials)] = {
    logger.debug("Registering user")
    client.dataRequest[SsoCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "username" -> req.username,
      "password" -> req.password
    )))) map extractUserId transform(identity, registrationErrorsTransformer)
  }

  def linkAccount(token: SsoAccessToken, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit] = {
    logger.debug("Linking account")
    client.unitRequest(Post(versioned(C.LinkUri), FormData(Map(
      "service_user_id" -> id.external,
      "service_allow_marketing" -> allowMarketing.toString,
      "service_tc_accepted_version" -> termsVersion
    ))), oauthCredentials(token)) transform(identity, linkErrorsTransformer)
  }

  def authenticate(c: PasswordCredentials): Future[SsoCredentials] = {
    logger.debug("Authenticating via password credentials")
    client.dataRequest[SsoCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.PasswordGrant,
      "username" -> c.username,
      "password" -> c.password
    )))) map extractUserId transform(_._2, authenticationErrorsTransformer)
  }

  def userInfo(token: SsoAccessToken): Future[UserInformation] = {
    logger.debug("Fetching user info")
    client.dataRequest[UserInformation](Get(versioned(C.UserInfoUri)), oauthCredentials(token)) transform(identity, userInfoErrorsTransformer)
  }

  def refresh(ssoRefreshToken: SsoRefreshToken): Future[SsoCredentials] = {
    logger.debug("Authenticating via refresh token")
    client.dataRequest[SsoCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RefreshTokenGrant,
      "refresh_token" -> ssoRefreshToken.value
    )))) transform(identity, refreshErrorsTransformer)
  }

  def revokeToken(token: SsoRefreshToken): Future[Unit] = {
    logger.debug("Revoking refresh token")
    client.unitRequest(Post(versioned(C.RevokeTokenUri), FormData(Map(
      "token" -> token.value
    )))) transform(identity, revokeTokenErrorsTransformer)
  }

  def sessionStatus(token: SsoAccessToken): Future[TokenStatus] = {
    logger.debug("Fetching session status")
    client.dataRequest[TokenStatus](Post(versioned(C.TokenStatusUri), FormData(Map(
      "token" -> token.value
    )))) transform(identity, tokenStatusErrorsTransformer)
  }

  def tokenStatus(token: SsoToken): Future[TokenStatus] = {
    logger.debug("Fetching token status")

    client.dataRequest[TokenStatus](Post(versioned(C.TokenStatusUri), FormData(Map(
      "token" -> token.value
    )))) transform(identity, tokenStatusErrorsTransformer)
  }

  def extendSession(token: SsoAccessToken): Future[Unit] = {
    logger.debug("Refreshing session")
    client.unitRequest(
      Post(versioned(C.ExtendSessionUri), FormData(Map.empty[String, String])),
      oauthCredentials(token)) transform(identity, extendSessionErrorTransformer)
  }

  def updateUser(token: SsoAccessToken, req: UserPatch): Future[Unit] = {
    logger.debug("Updating user")

    val formData = Seq(
      req.username.map("username" -> _),
      req.first_name.map("first_name" -> _),
      req.last_name.map("last_name" -> _),
      req.allow_marketing_communications.map("service_allow_marketing" -> _.toString)
    ).flatten

    client.unitRequest(
      Patch(versioned(C.UserInfoUri), FormData(formData)), oauthCredentials(token)) transform(identity, updateUserErrorTransformer)
  }

  def updatePassword(token: SsoAccessToken, oldPassword: String, newPassword: String): Future[Unit] = {
    logger.debug("Changing password")

    client.unitRequest(Post(versioned(C.UpdatePasswordUri), FormData(Map(
      "old_password" -> oldPassword,
      "new_password" -> newPassword
    ))), oauthCredentials(token)) transform(identity, updatePasswordErrorTransformer)
  }

  def generatePasswordResetToken(username: String): Future[SsoPasswordResetTokenResponse] = {
    logger.debug("Generate password-reset token")

    client.dataRequest[SsoPasswordResetTokenResponse](Post(versioned(C.GeneratePasswordResetTokenUri), FormData(Map(
      "username" -> username
    )))) transform(identity, generatePasswordTokenErrorTransformer)
  }

  def resetPassword(passwordToken: SsoPasswordResetToken, newPassword: String): Future[SsoUserCredentials] = {
    logger.debug("Reset password")

    client.dataRequest[SsoCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.PasswordResetTokenGrant,
      "password_reset_token" -> passwordToken.value,
      "password" -> newPassword
    )))) map(userCredentials) transform(identity, resetPasswordErrorTransformer)
  }
}
