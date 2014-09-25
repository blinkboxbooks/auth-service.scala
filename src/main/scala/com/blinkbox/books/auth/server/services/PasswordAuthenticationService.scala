package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data.{Client, _}
import com.blinkbox.books.auth.server.events.{Publisher, UserAuthenticated, UserRegistered, UserUpdated}
import com.blinkbox.books.auth.server.sso._
import com.blinkbox.books.slick.DatabaseSupport
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile
import scala.util.Random

trait PasswordAuthenticationService {
  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]
}

class DefaultPasswordAuthenticationService[Profile <: BasicProfile, Database <: Profile#Backend#Database](
    db: Database,
    authRepo: AuthRepository[Profile],
    userRepo: UserRepository[Profile],
    tokenBuilder: TokenBuilder,
    events: Publisher,
    ssoSync: SsoSyncService,
    sso: Sso)(implicit executionContext: ExecutionContext, clock: Clock) extends PasswordAuthenticationService with ClientAuthenticator[Profile] {

  private def findUser(username: String): Future[Option[User]] = Future {
    db.withSession { implicit session => userRepo.userWithUsername(username) }
  }

  private def getClient(credentials: PasswordCredentials, user: User): Future[Option[Client]] = Future {
    db.withSession { implicit session => authenticateClient(authRepo, credentials, user.id) }
  }

  private def getToken(userId: UserId, clientId: Option[ClientId], refreshToken: SsoRefreshToken) : Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.createRefreshToken(userId, clientId, refreshToken) }
  }

  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = {
    val ssoAuthenticationFuture = sso authenticate (credentials) map (_.credentials)

    for {
      ssoCredentials  <- ssoAuthenticationFuture
      maybeUser       <- findUser(credentials.username)
      user            <- ssoSync(maybeUser, ssoCredentials.accessToken)
      client          <- getClient(credentials, user)
      token           <- getToken(user.id, client.map(_.id), ssoCredentials.refreshToken)
      _               <- events.publish(UserAuthenticated(user, client))
    } yield tokenBuilder.issueAccessToken(user, client, token, Some(ssoCredentials), includeRefreshToken = true)

  } transform(identity, {
    case SsoUnauthorized => Failures.invalidUsernamePassword
    case SsoInvalidRequest(msg) => Failures.requestException(msg, InvalidRequest)
    case SsoTooManyRequests(retryAfter) => Failures.tooManyRequests(retryAfter)
    case e: Throwable => e
  })
}
