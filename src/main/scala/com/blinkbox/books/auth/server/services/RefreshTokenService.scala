package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{Publisher, UserAuthenticated}
import com.blinkbox.books.auth.server.sso.{SSOCredentials, SSO}
import com.blinkbox.books.slick.DBTypes
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait RefreshTokenService {
  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]
}

class DefaultRefreshTokenService[DB <: DBTypes](
    db: DB#Database,
    authRepo: AuthRepository[DB#Profile],
    userRepo: UserRepository[DB#Profile],
    clientRepo: ClientRepository[DB#Profile],
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock, tag: ClassTag[DB#ConstraintException])
  extends RefreshTokenService with ClientAuthenticator[DB#Profile] {

  private def fetchRefreshToken(c: RefreshTokenCredentials): Future[RefreshToken] = Future {
    db.withSession { implicit session => authRepo.refreshTokenWithToken(c.token).getOrElse(throw Failures.invalidRefreshToken)}
  }

  private def fetchSSOCredentials(token: RefreshToken): Future[Option[SSOCredentials]] = token.ssoRefreshToken match {
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
    val client = db.withSession { implicit session => authenticateClient(authRepo, credentials, token.userId) }
    checkAuthorization(token, client)
    client
  }

  private def extendTokenLifetime(token: RefreshToken): Future[Unit] = Future {
    db.withSession { implicit session => authRepo.extendRefreshTokenLifetime(token)}
  }

  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = {
    val tokenFuture = fetchRefreshToken(credentials).flatMap { token =>
      val ssoFuture = fetchSSOCredentials(token)
      val userFuture = fetchUser(token.userId)
      val clientFuture = fetchClient(credentials, token)

      for {
        ssoCreds  <- ssoFuture
        user      <- userFuture
        client    <- clientFuture
      } yield (token, user, client, TokenBuilder.issueAccessToken(user, client, token, ssoCreds))
    }

    tokenFuture.onSuccess {
      case (token, user, client, _) =>
        extendTokenLifetime(token)
        events.publish(UserAuthenticated(user, client))
    }

    tokenFuture.map(_._4)
  }
}
