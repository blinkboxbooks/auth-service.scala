package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{Publisher, UserAuthenticated}
import com.blinkbox.books.auth.server.sso.SSO
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
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock, tag: ClassTag[DB#ConstraintException]) extends RefreshTokenService {

  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = Future {
//    val (user, client, token) = db.withTransaction { implicit transaction =>
//      val t = authRepo.refreshTokenWithToken(credentials.token).getOrElse(throw Failures.invalidRefreshToken)
//      val u = userRepo.userWithId(t.userId).getOrElse(throw Failures.invalidRefreshToken)
//      val c = credentials.asPair.flatMap { case (id, secret) => authRepo.authenticateClient(id, secret, u.id) }
//
//      (t.clientId, c) match {
//        case (None, Some(client)) => authRepo.associateRefreshTokenWithClient(t, client) // Token needs to be associated with the client
//        case (None, None) => // Do nothing: token isn't associated with a client and there is no client
//        case (Some(tId), Some(client)) if (tId == client.id) => // Do nothing: token is associated with the right client
//        case _ => throw Failures.refreshTokenNotAuthorized
//      }
//
//      authRepo.extendRefreshTokenLifetime(t)
//      (u, c, t)
//    }
//    events.publish(UserAuthenticated(user, client))
//    TokenBuilder.issueAccessToken(user, client, token, ???) // TODO: Put SSO credentials here
    ???
  }
}
