package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.{UserRegistration, SSOConfig}
import spray.client.pipelining._
import spray.http.FormData

import scala.concurrent.Future

object SSOConstants {
  val TokenUri = "/oauth2/token"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
}

trait SSO {
  def register(req: UserRegistration): Future[TokenCredentials]
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
  import com.blinkbox.books.auth.server.sso.Serialization._

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SSOConstants

  def register(req: UserRegistration): Future[TokenCredentials] =
    client.dataRequest[TokenCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "username" -> req.username,
      "password" -> req.password
    ))))
}
