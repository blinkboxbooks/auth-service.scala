package com.blinkbox.books.auth.server.sso

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import spray.http.HttpCredentials

trait SSO {
  def perform[Req, Resp](req: Req)(implicit executor: SSOExecutor[Req, Resp]): Future[Resp]

  // These are a method representation of the SSO Api; remove them if the typeclass approach works fine

  // User - unauthenticated
  // def register(reg: RegisterUser): Future[TokenCredentials]
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

class DefaultSSO extends SSO {
  def perform[Req, Resp](req: Req)(implicit executor: SSOExecutor[Req, Resp]): Future[Resp] = executor(req)
}
