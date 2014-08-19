package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events._
import com.blinkbox.books.auth.server.sso.{SSOUnauthorized, SSO, SSOAccessToken, SSOTokenElevation}
import com.blinkbox.books.auth.{Elevation, User => AuthenticatedUser}
import com.blinkbox.books.time.Clock
import shapeless.Typeable._

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait SessionService {
  def extendSession()(implicit user: AuthenticatedUser): Future[SessionInfo]
  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo]
}

class DefaultSessionService[Profile <: BasicProfile, Database <: Profile#Backend#Database](
    db: Database,
    authRepo: AuthRepository[Profile],
    userRepo: UserRepository[Profile],
    clientRepo: ClientRepository[Profile],
    geoIP: GeoIP,
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock)
  extends SessionService with UserInfoFactory with ClientInfoFactory with ClientAuthenticator[Profile] {

  // TODO: Make this configurable
  val MaxClients = 12

  private def sessionInfoFromUser(user: AuthenticatedUser) = fetchRefreshToken(user).map { token =>
    val elevation = (token.isValid, user.claims.isDefinedAt("sso/at")) match {
      case (false, _) => None
      case (true, true) => Some(token.elevation)
      case (true, false) => Some(Elevation.Unelevated)
    }

    SessionInfo(
      token_status = token.status,
      token_elevation = elevation,
      token_elevation_expires_in = if (token.isValid && elevation != Some(Elevation.Unelevated)) Some(token.elevationDropsIn.toSeconds) else None
      // TODO: Roles
    )
  }

  private def elevationFromSessionElevation(e: SSOTokenElevation) = e match {
    case SSOTokenElevation.Critical => Elevation.Critical
    case SSOTokenElevation.None => Elevation.Unelevated
  }

  private def querySessionWithSSO(token: SSOAccessToken) = sso.sessionStatus(token).map { s =>
    val status = TokenStatus.fromSSOValidity(s.status)
    val elevation = if (status == TokenStatus.Valid) Some(elevationFromSessionElevation(s.sessionElevation)) else None

    SessionInfo(
      token_status = status,
      token_elevation = elevation,
      token_elevation_expires_in = elevation.flatMap(e => if (e != Elevation.Unelevated) Some(s.sessionElevationExpiresIn) else None)
      // TODO: Roles
    )
  }

  private def fetchRefreshToken(user: AuthenticatedUser): Future[RefreshToken] = Future {
    val tokenId = user.
      claims.
      get("zl/rti").
      flatMap(_.cast[Int]).
      map(RefreshTokenId.apply).
      getOrElse(throw Failures.invalidRefreshToken)

    db.withSession(implicit session => authRepo.refreshTokenWithId(tokenId)).getOrElse(throw Failures.unverifiedIdentity)
  }

  override def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = user.claims.get("sso/at").
    flatMap(_.cast[String]).
    map(SSOAccessToken.apply).
    fold(sessionInfoFromUser(user))(querySessionWithSSO)

  override def extendSession()(implicit user: AuthenticatedUser): Future[SessionInfo] = (for {
    raw <- user.claims.get("sso/at")
    at  <- raw.cast[String]
  } yield {
    sso
      .extendSession(SSOAccessToken(at))
      .flatMap(_ => querySession())
      .transform(identity, { case SSOUnauthorized => Failures.unverifiedIdentity })
  }).getOrElse(Future.failed(Failures.unverifiedIdentity))
}
