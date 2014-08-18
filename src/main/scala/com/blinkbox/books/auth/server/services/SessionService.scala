package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events._
import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSO}
import com.blinkbox.books.auth.{User => AuthenticatedUser, Elevation}
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait SessionService {
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

  private def sessionInfoFromRefreshToken(token: RefreshToken) = SessionInfo(
    token_status = token.status,
    token_elevation = if (token.isValid) Some(token.elevation) else None,
    token_elevation_expires_in = if (token.isValid && token.elevation != Elevation.Unelevated) Some(token.elevationDropsIn.toSeconds) else None
    // TODO: Roles
  )

  private def elevationFromSessionElevation(e: SSOTokenElevation) = e match {
    case SSOTokenElevation.Critical => Elevation.Critical
    case SSOTokenElevation.None => Elevation.Unelevated
  }

  private def querySessionWithSSO(ssoToken: String) = sso.tokenStatus(ssoToken).map { s =>
    val status = RefreshTokenStatus.fromSSOValidity(s.status)
    val elevation = if (status == RefreshTokenStatus.Valid) Some(elevationFromSessionElevation(s.sessionElevation)) else None

    SessionInfo(
      token_status = status,
      token_elevation = elevation,
      token_elevation_expires_in = elevation.flatMap(e => if (e != Elevation.Unelevated) Some(s.sessionElevationExpiresIn) else None)
      // TODO: Roles
    )
  }

  private def fetchRefreshToken(user: AuthenticatedUser): Future[RefreshToken] = Future {
    // TODO: Account for casting failure
    val tokenId = user.
      claims.
      get("zl/rti").
      map(i => RefreshTokenId(i.asInstanceOf[String].toInt)).
      getOrElse(throw Failures.invalidRefreshToken)

    db.withSession(implicit session => authRepo.refreshTokenWithId(tokenId)).getOrElse(throw Failures.unverifiedIdentity)
  }

  override def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = for {
    rt <- fetchRefreshToken(user)
    si <- rt.ssoRefreshToken.fold(Future.successful(sessionInfoFromRefreshToken(rt)))(t => querySessionWithSSO(t))
  } yield si
}