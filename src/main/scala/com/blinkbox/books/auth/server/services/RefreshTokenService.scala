package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{Publisher, UserAuthenticated}
import com.blinkbox.books.auth.server.sso.{SsoRefreshToken, Sso, SsoCredentials}
import com.blinkbox.books.slick.DatabaseSupport
import com.blinkbox.books.time.Clock
import org.joda.time.Duration

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait RefreshTokenService {
  def revokeRefreshToken(token: String): Future[Unit]
  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]
}

class DefaultRefreshTokenService[DB <: DatabaseSupport](
    db: DB#Database,
    authRepo: AuthRepository[DB#Profile],
    userRepo: UserRepository[DB#Profile],
    clientRepo: ClientRepository[DB#Profile],
    tokenLifetimeExtension: FiniteDuration,
    tokenBuilder: TokenBuilder,
    events: Publisher,
    sso: Sso)(implicit executionContext: ExecutionContext, clock: Clock)
  extends RefreshTokenService with ClientAuthenticator[DB#Profile] {

  private def fetchRefreshToken(c: RefreshTokenCredentials): Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.refreshTokenWithToken(c.token).getOrElse(throw Failures.invalidRefreshToken)}
  }

  private def fetchSSOCredentials(token: RefreshToken): Future[Option[SsoCredentials]] = token.ssoRefreshToken match {
    case Some(t) => sso.refresh(t).map(Option.apply)
    case None => Future.successful(None)
  }

  private def fetchUser(id: UserId): Future[User] = Future {
    db.withSession { implicit session => userRepo.userWithId(id).getOrElse(throw Failures.invalidRefreshToken)}
  }

  private def checkAuthorization(token: RefreshToken, client: Option[Client]): Future[Unit] = Future {
    (token.clientId, client) match {
      case (None, Some(client)) => // Token needs to be associated with the client
        db.withSession { implicit session => authRepo.associateRefreshTokenWithClient(token, client)}
      case (None, None) => // Do nothing: token isn't associated with a client and there is no client
      case (Some(tId), Some(client)) if (tId == client.id) => // Do nothing: token is associated with the right client
      case _ => throw Failures.refreshTokenNotAuthorized
    }
  }

  private def fetchClient(credentials: ClientCredentials, token: RefreshToken): Future[Option[Client]] = Future {
    db.withSession { implicit session => authenticateClient(authRepo, credentials, token.userId) }
  }

  private def extendTokenLifetime(token: RefreshToken, ssoRefreshToken: Option[SsoRefreshToken]): Future[Unit] = Future {
    val timeSpan = Duration.millis(tokenLifetimeExtension.toMillis)
    db.withSession { implicit session => authRepo.extendRefreshTokenLifetime(token, ssoRefreshToken, timeSpan)}
  }

  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = {
    val tokenFuture = fetchRefreshToken(credentials).flatMap { token =>
      val ssoFuture = fetchSSOCredentials(token)
      val userFuture = fetchUser(token.userId)
      val clientFuture = fetchClient(credentials, token)

      for {
        client    <- clientFuture
        _         <- checkAuthorization(token, client)
        ssoCreds  <- ssoFuture
        user      <- userFuture
      } yield (token, user, client, ssoCreds, tokenBuilder.issueAccessToken(user, client, token, ssoCreds))
    }

    tokenFuture.onSuccess {
      case (token, user, client, ssoCreds, _) =>
        // TODO: The result of this Future is just discarded, there should be better tracking of errors
        extendTokenLifetime(token, ssoCreds.map(_.refreshToken))
        events.publish(UserAuthenticated(user, client))
    }

    tokenFuture.map(_._5)
  }

  override def revokeRefreshToken(token: String): Future[Unit] = {
    val zuulToken = Future {
      db.withSession { implicit session =>
        authRepo.refreshTokenWithToken(token).getOrElse(throw Failures.invalidRefreshToken)
      }
    }

    val ssoRevocation = zuulToken flatMap { r => r.ssoRefreshToken.fold(Future.successful())(ssoT => sso.revokeToken(ssoT)) }

    ssoRevocation flatMap { _ =>
      zuulToken.map { zt =>
        db.withSession { implicit session => authRepo.revokeRefreshToken(zt) }
      }
    }
  }
}
