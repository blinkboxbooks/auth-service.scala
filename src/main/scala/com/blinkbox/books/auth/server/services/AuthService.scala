package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events._
import com.blinkbox.books.auth.server.sso.SSO
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait AuthService {
  def revokeRefreshToken(token: String): Future[Unit]
  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]
  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]
  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo]
}

trait GeoIP {
  def countryCode(address: RemoteAddress): String
}

class DefaultAuthService[Profile <: BasicProfile, Database <: Profile#Backend#Database](
    db: Database,
    authRepo: AuthRepository[Profile],
    userRepo: UserRepository[Profile],
    clientRepo: ClientRepository[Profile],
    geoIP: GeoIP,
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock)
  extends AuthService with UserInfoFactory with ClientInfoFactory {

  // TODO: Make this configurable
  val MaxClients = 12

  override def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    val (user, client, token) = db.withTransaction { implicit transaction =>
      val u = authenticateUser(credentials, clientIP)
      val c = authenticateClient(credentials, u)
      val t = authRepo.createRefreshToken(u.id, c.map(_.id), ???) // TODO: Put the SSO token here
      (u, c, t)
    }
    events.publish(UserAuthenticated(user, client))
    TokenBuilder.issueAccessToken(user, client, token, ???, includeRefreshToken = true) // TODO: Put SSO credentials here
  }

  override def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = Future {
    val (user1, client1, token1) = db.withTransaction { implicit transaction =>
      val t = authRepo.refreshTokenWithToken(credentials.token).getOrElse(throw Failures.invalidRefreshToken)
      val u = userRepo.userWithId(t.userId).getOrElse(throw Failures.invalidRefreshToken)
      val c = authenticateClient(credentials, u)

      (t.clientId, c) match {
        case (None, Some(client)) => authRepo.associateRefreshTokenWithClient(t, client) // Token needs to be associated with the client
        case (None, None) => // Do nothing: token isn't associated with a client and there is no client
        case (Some(tId), Some(client)) if (tId == client.id) => // Do nothing: token is associated with the right client
        case _ => throw Failures.refreshTokenNotAuthorized
      }

      authRepo.extendRefreshTokenLifetime(t)
      (u, c, t)
    }
    events.publish(UserAuthenticated(user1, client1))
    TokenBuilder.issueAccessToken(user1, client1, token1, ???) // TODO: Put SSO credentials here
  }

  override def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = Future {
    // TODO: This line should be re-written to avoid the `get` invocation on the option and to account for casting failure
    val tokenId = RefreshTokenId(user.claims.get("zl/rti").get.asInstanceOf[Int])

    val token = db.withSession(implicit session => authRepo.refreshTokenWithId(tokenId)).getOrElse(throw Failures.unverifiedIdentity)
    SessionInfo(
      token_status = token.status,
      token_elevation = if (token.isValid) Some(token.elevation) else None,
      token_elevation_expires_in = if (token.isValid) Some(token.elevationDropsIn.toSeconds) else None
      // TODO: Roles
    )
  }

  override def revokeRefreshToken(token: String): Future[Unit] = Future {
    db.withSession { implicit session =>
      val retrievedToken = authRepo.refreshTokenWithToken(token).getOrElse(throw Failures.invalidRefreshToken)
      authRepo.revokeRefreshToken(retrievedToken)
    }
  }

  private def authenticateUser(credentials: PasswordCredentials, clientIP: Option[RemoteAddress])(implicit session: authRepo.Session): User = {
    val user = userRepo.userWithUsernameAndPassword(credentials.username, credentials.password)
    authRepo.recordLoginAttempt(credentials.username, user.isDefined, clientIP)

    user.getOrElse(throw Failures.invalidUsernamePassword)
  }

  private def authenticateClient(credentials: ClientCredentials, user: User)(implicit session: authRepo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield authRepo.
      authenticateClient(clientId, clientSecret, user.id).
      getOrElse(throw Failures.invalidClientCredentials)
}
