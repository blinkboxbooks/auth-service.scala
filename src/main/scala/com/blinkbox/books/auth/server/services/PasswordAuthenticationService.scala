package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data.{Client, _}
import com.blinkbox.books.auth.server.events.{Publisher, UserAuthenticated, UserRegistered, UserUpdated}
import com.blinkbox.books.auth.server.sso._
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
    clientRepo: ClientRepository[Profile],
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock) extends PasswordAuthenticationService {

  // TODO: Move this to configuration
  val TermsVersion = "1.0"

  private def findUser(username: String): Future[Option[User]] = Future {
    db.withSession { implicit session => userRepo.userWithUsername(username) }
  }

  private def registerUser(reg: UserRegistration): Future[User] = Future {
    db.withSession { implicit session => userRepo.createUser(reg) }
  }

  private def updateUser(user: User): Future[Unit] = Future {
    db.withSession { implicit session => userRepo.updateUser(user) }
  }

  private def createFromSSO(ssoAccessToken: SSOAccessToken): Future[User] = {
    val registrationFuture = sso.userInfo(ssoAccessToken).map { u =>
      (u.userId, UserRegistration(u.firstName, u.lastName, u.username, Random.nextString(32), true, false, None, None, None, None))
    }

    for {
      (ssoId, registration) <- registrationFuture
      user                  <- registerUser(registration)
      _                     <- sso linkAccount(ssoAccessToken, user.id, false, TermsVersion)
      linkedUser            =  user.copy(ssoId = Some(ssoId))
      _                     <- updateUser(linkedUser)
      _                     <- events.publish(UserRegistered(linkedUser))
    } yield linkedUser
  }

  private def updateFromSSO(ssoAccessToken: SSOAccessToken, user: User): Future[User] = {
    val userFuture = sso.userInfo(ssoAccessToken).map { u =>
      user.copy(
        firstName = u.firstName,
        lastName = u.lastName,
        ssoId = Some(u.userId))
    }

    for {
      updatedUser <- userFuture
      _           <- updateUser(updatedUser)
      _           <- events.publish(UserUpdated(user, updatedUser))
    } yield updatedUser
  }

  private def ensureLinked(maybeUser: Option[User], ssoAccessToken: SSOAccessToken): Future[User] =
    maybeUser.fold(createFromSSO(ssoAccessToken)) { user =>
      if (user.ssoId.isDefined) Future.successful(user)
      else for {
        _           <- sso linkAccount(ssoAccessToken, user.id, user.allowMarketing, TermsVersion)
        updatedUser <- updateFromSSO(ssoAccessToken, user)
      } yield updatedUser
    }

  private def getClient(credentials: PasswordCredentials, user: User): Future[Option[Client]] = Future {
    db.withSession { implicit session => authenticateClient(credentials, user) }
  }

  private def getToken(userId: UserId, clientId: Option[ClientId], refreshToken: String) : Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.createRefreshToken(userId, clientId, refreshToken) }
  }

  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = {
    val ssoAuthenticationFuture = sso authenticate (credentials)

    for {
      ssoCredentials  <- ssoAuthenticationFuture
      maybeUser       <- findUser(credentials.username)
      user            <- ensureLinked(maybeUser, ssoCredentials.accessToken)
      client          <- getClient(credentials, user)
      token           <- getToken(user.id, client.map(_.id), ssoCredentials.refreshToken)
      _               <- events.publish(UserAuthenticated(user, client))
    } yield TokenBuilder.issueAccessToken(user, client, token, Some(ssoCredentials), includeRefreshToken = true)

  } transform(identity, {
    case SSOUnauthorized => Failures.invalidUsernamePassword
    case SSOInvalidRequest(msg) => Failures.requestException(msg, InvalidRequest)
    case SSOTooManyRequests(retryAfter) => Failures.tooManyRequests(retryAfter)
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
