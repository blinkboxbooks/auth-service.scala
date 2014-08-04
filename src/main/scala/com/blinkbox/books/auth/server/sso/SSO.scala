package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.SSOConfig
import com.blinkbox.books.auth.server.data.UserId
import spray.client.pipelining._
import spray.http.FormData

import scala.concurrent.Future

object SSOConstants {
  val TokenUri = "/oauth2/token"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
}

trait SSO {
  def register(reg: RegisterUser): Future[TokenCredentials]
  // def authenticate(credentials: AuthenticateUser): Future[TokenCredentials]
  // def refresh(token: RefreshToken): Future[TokenCredentials]
  // def resetPassword(token: PasswordResetToken): Future[TokenCredentials]
  // def revokeToken(token: RevokeToken): Future[Unit]
  // // User - authenticated
  // def linkAccount(link: LinkAccount): Future[Unit]
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

class DefaultSSO(config: SSOConfig, client: Client) extends SSO {
  import Serialization._

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SSOConstants

  def register(req: RegisterUser): Future[TokenCredentials] =
    client.dataRequest[TokenCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "username" -> req.username,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "service_user_id" -> req.id.value.toString,
      "service_allow_marketing" -> (if (req.allowMarketing) "true" else "false"),
      "service_tc_accepted_version" -> req.acceptedTermsVersion
    ))))
}