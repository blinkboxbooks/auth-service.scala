package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.{UserRegistration, SSOConfig}
import spray.client.pipelining._
import spray.http.FormData

import scala.concurrent.{ExecutionContext, Future}

sealed trait SSOException extends Throwable
case class InvalidAccessToken(receivedCredentials: SSOCredentials) extends SSOException

object SSOConstants {
  val TokenUri = "/oauth2/token"
  val LinkUri = "/link"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
}

trait SSO {
  def register(req: UserRegistration): Future[SSOCredentials]
  // def authenticate(credentials: AuthenticateUser): Future[TokenCredentials]
  // def refresh(token: RefreshToken): Future[TokenCredentials]
  // def resetPassword(token: PasswordResetToken): Future[TokenCredentials]
  // def revokeToken(token: RevokeToken): Future[Unit]
  // // User - authenticated
  def linkAccount(id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit]
  // def generatePasswordReset(gen: GeneratePasswordReset): Future[PasswordResetCredentials]
  // def updatePassword(update: UpdatePassword): Future[Unit]
  // def tokenStatus(req: GetTokenStatus): Future[TokenStatus]
  // def refreshSession(): Future[Unit]
  // def userInfo(): Future[UserInformation]
  // def updateUser(req: PatchUser): Future[Unit]
  // // Admin
  // def adminSearchUser(req: SearchUser): Future[SearchUserResult]
  // def adminUserDetails(req: GetUserDetails): Future[UserDetail]
  // def adminUpdateUser(req: UpdateUser): Future[UserDetail]
  // // Health-check
  // def systemStatus(): Future[SystemStatus]
}

class DefaultSSO(config: SSOConfig, client: Client, tokenDecoder: SsoAccessTokenDecoder)(implicit ec: ExecutionContext) extends SSO {
  import com.blinkbox.books.auth.server.sso.Serialization._

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SSOConstants

  private def validateToken(cred: SSOCredentials): SSOCredentials =
    if (SsoAccessToken.decode(cred.accessToken, tokenDecoder).isSuccess) cred
    else throw new InvalidAccessToken(cred)

  // TODO: Put some real implementations here, the return type should always be an SSOException
  private def commonErrorsTransformer: PartialFunction[Throwable, Throwable] = { case e: Throwable => e }
  private def registrationErrorsTransformer = ((_: Throwable) match { case e: Throwable => e }) andThen commonErrorsTransformer

  def register(req: UserRegistration): Future[SSOCredentials] =
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "username" -> req.username,
      "password" -> req.password
    )))) map validateToken transform(identity, registrationErrorsTransformer)

  def linkAccount(id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit] =
    client.unitRequest(Post(versioned(C.LinkUri), FormData(Map(
      "service_user_id" -> id.external,
      "service_allow_marketing" -> allowMarketing.toString,
      "service_tc_accepted_version" -> termsVersion
    ))))
}
