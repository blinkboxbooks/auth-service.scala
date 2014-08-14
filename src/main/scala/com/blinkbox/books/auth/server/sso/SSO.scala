package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.{RefreshTokenCredentials, PasswordCredentials, UserRegistration, SSOConfig}
import org.json4s.JsonAST.{JString, JField, JObject}
import org.slf4j.LoggerFactory
import spray.client.pipelining._
import spray.http.{HttpCredentials, StatusCodes, OAuth2BearerToken, FormData}
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

sealed trait SSOException extends Throwable
case class SSOInvalidAccessToken(receivedCredentials: SSOCredentials) extends SSOException
case object SSOUnauthorized extends SSOException
case object SSOConflict extends SSOException
case class SSOTooManyRequests(retryAfter: FiniteDuration) extends SSOException
case class SSOInvalidRequest(message: String) extends SSOException
case class SSOUnknownException(e: Throwable) extends SSOException

object SSOConstants {
  val TokenUri = "/oauth2/token"
  val LinkUri = "/link"
  val InfoUri = "/user"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
  val PasswordGrant = "password"
  val RefreshTokenGrant = "refresh_token"
}

trait SSO {
  def register(req: UserRegistration): Future[(String, SSOCredentials)]
  def authenticate(c: PasswordCredentials): Future[SSOCredentials]
  def refresh(ssoRefreshToken: String): Future[SSOCredentials]
  // def resetPassword(token: PasswordResetToken): Future[TokenCredentials]
  // def revokeToken(token: RevokeToken): Future[Unit]
  // // User - authenticated
  def linkAccount(ssoCredentials: SSOCredentials, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit]
  // def generatePasswordReset(gen: GeneratePasswordReset): Future[PasswordResetCredentials]
  // def updatePassword(update: UpdatePassword): Future[Unit]
  // def tokenStatus(req: GetTokenStatus): Future[TokenStatus]
  // def refreshSession(): Future[Unit]
  def userInfo(ssoCredentials: SSOCredentials): Future[UserInformation]
  // def updateUser(req: PatchUser): Future[Unit]
  // // Admin
  // def adminSearchUser(req: SearchUser): Future[SearchUserResult]
  // def adminUserDetails(req: GetUserDetails): Future[UserDetail]
  // def adminUpdateUser(req: UpdateUser): Future[UserDetail]
  // // Health-check
  // def systemStatus(): Future[SystemStatus]
}

class DefaultSSO(config: SSOConfig, client: Client, tokenDecoder: SsoAccessTokenDecoder)(implicit ec: ExecutionContext) extends SSO {
  import com.blinkbox.books.auth.server.sso.Serialization.json4sUnmarshaller

  val log = LoggerFactory.getLogger(getClass)

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SSOConstants

  private def extractUserId(cred: SSOCredentials): (String, SSOCredentials) = SsoAccessToken.decode(cred.accessToken, tokenDecoder) match {
    case Success(token) => (token.subject, cred)
    case Failure(_) => throw new SSOInvalidAccessToken(cred)
  }

  private def extractInvalidRequest(e: UnsuccessfulResponseException): SSOException = {
    import org.json4s.jackson.JsonMethods._
    parseOpt(e.response.entity.asString).collect {
      case JObject(
        JField("error", JString("invalid_request")) ::
        JField("error_description", JString(s)) :: Nil) => SSOInvalidRequest(s)
    } getOrElse(SSOUnknownException(e))
  }

  private def extractTooManyRequests(e: UnsuccessfulResponseException): SSOException = try {
    import scala.concurrent.duration._
    e.response.
      headers.
      find(_.lowercaseName == "retry-after").
      map(r => SSOTooManyRequests(r.value.toInt.seconds)).
      getOrElse(SSOUnknownException(e))
  } catch {
    case e: NumberFormatException => SSOUnknownException(e)
  }

  private def commonErrorsTransformer: Throwable => SSOException = {
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Unauthorized => SSOUnauthorized
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Conflict => SSOConflict
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.BadRequest => extractInvalidRequest(e)
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.TooManyRequests => extractTooManyRequests(e)
    case e: Throwable  => SSOUnknownException(e)
  }

  // TODO: These transformers should deal with some specific exception and then forward to common for unhandled ones
  private def registrationErrorsTransformer = commonErrorsTransformer
  private def authenticationErrorsTransformer = commonErrorsTransformer
  private def linkErrorsTransformer = commonErrorsTransformer
  private def refreshErrorsTransformer = commonErrorsTransformer
  private def userInfoErrorsTransformer = commonErrorsTransformer

  def oauthCredentials(ssoCredentials: SSOCredentials): HttpCredentials = new OAuth2BearerToken(ssoCredentials.accessToken)

  def register(req: UserRegistration): Future[(String, SSOCredentials)] = {
    log.debug("Registering user", req)
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "username" -> req.username,
      "password" -> req.password
    )))) map extractUserId transform(identity, registrationErrorsTransformer)
  }

  def linkAccount(ssoCredentials: SSOCredentials, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit] = {
    log.debug("Linking account", ssoCredentials, id)
    client.unitRequest(Post(versioned(C.LinkUri), FormData(Map(
      "service_user_id" -> id.external,
      "service_allow_marketing" -> allowMarketing.toString,
      "service_tc_accepted_version" -> termsVersion
    ))), oauthCredentials(ssoCredentials)) transform(identity, linkErrorsTransformer)
  }

  def authenticate(c: PasswordCredentials): Future[SSOCredentials] = {
    log.debug("Authenticating via password credentials", c)
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.PasswordGrant,
      "username" -> c.username,
      "password" -> c.password
    )))) map extractUserId transform(_._2, authenticationErrorsTransformer)
  }

  def userInfo(ssoCredentials: SSOCredentials): Future[UserInformation] = {
    log.debug("Fetching user info", ssoCredentials)
    client.dataRequest[UserInformation](Get(versioned(C.InfoUri)), oauthCredentials(ssoCredentials)) transform(identity, userInfoErrorsTransformer)
  }

  def refresh(ssoRefreshToken: String): Future[SSOCredentials] = {
    log.debug("Authenticating via refresth token", ssoRefreshToken)
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RefreshTokenGrant,
      "refresh_token" -> ssoRefreshToken
    )))) transform(identity, refreshErrorsTransformer)
  }
}
