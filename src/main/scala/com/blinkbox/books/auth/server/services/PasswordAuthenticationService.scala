package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{UserAuthenticated, Publisher}
import com.blinkbox.books.auth.server.sso.{Unauthorized, SSOCredentials, SSO}
import com.blinkbox.books.auth.server._
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait PasswordAuthenticationService {
  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]
}

class DefaultPasswordAuthenticationService[Profile <: BasicProfile, Database <: Profile#Backend#Database](
    db: Database,
    authRepo: AuthRepository[Profile],
    userRepo: UserRepository[Profile],
    clientRepo: ClientRepository[Profile],
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock) extends PasswordAuthenticationService {

  val TermsVersion = "1.0"

  def findUser(username: String): Future[Option[User]] = Future {
    db.withSession { implicit session => userRepo.userWithUsername(username) }
  }

  // TODO: Get user info from SSO, create user and fire event
  def createUser(ssoCredentials: SSOCredentials): Future[User] = ???

  def updateFromSSO(user: User): Future[User] = Future {
    val linkedUser = user.copy(ssoLinked = true)
    // TODO: Get user info from SSO, update user and fire event
    db.withSession { implicit session => userRepo.updateUser(linkedUser) }
    linkedUser
  }

  def ensureLinked(maybeUser: Option[User], ssoCredentials: SSOCredentials): Future[User] =
    maybeUser.fold(createUser(ssoCredentials)) { user =>
      if (user.ssoLinked) Future.successful(user)
      else for {
        _           <- sso.linkAccount(ssoCredentials, user.id, false, TermsVersion)
        updatedUser <- updateFromSSO(user)
      } yield updatedUser
    }

  def getUser(credentials: PasswordCredentials, ssoCredentials: SSOCredentials): Future[User] =
    for {
      maybeUser <- findUser(credentials.username)
      user <- ensureLinked(maybeUser, ssoCredentials)
    } yield user

  def getClient(credentials: PasswordCredentials, user: User): Future[Option[Client]] = Future {
    db.withSession { implicit session => authenticateClient(credentials, user) }
  }

  def getToken(userId: UserId, clientId: Option[ClientId], refreshToken: String) : Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.createRefreshToken(userId, clientId, refreshToken) }
  }

  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = {
    // TODO: authRepo.recordLoginAttempt(credentials.username, user.isDefined, clientIP)
    for {
      ssoCredentials  <- sso authenticate (credentials)
      user            <- getUser(credentials, ssoCredentials)
      client          <- getClient(credentials, user)
      token           <- getToken(user.id, client.map(_.id), ssoCredentials.refreshToken)
      _               <- events.publish(UserAuthenticated(user, client))
    } yield TokenBuilder.issueAccessToken(user, client, token, ssoCredentials, includeRefreshToken = true)

  } transform(identity, {
    case Unauthorized => Failures.invalidUsernamePassword
    case e: Throwable => e
  })

  // TODO: Remove this duplication when working on the refresh token authentication scenario
  private def authenticateClient(credentials: ClientCredentials, user: User)(implicit session: authRepo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield authRepo.
      authenticateClient(clientId, clientSecret, user.id).
      getOrElse(throw Failures.invalidClientCredentials)
}
