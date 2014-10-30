package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.{Elevation, ElevationChecker, User}
import com.blinkbox.books.spray.InvalidTokenStatusException

import scala.concurrent.{ExecutionContext, Future}

class SsoElevationChecker(sso: Sso)(implicit executionContext: ExecutionContext) extends ElevationChecker {
  override def apply(user: User): Future[Elevation.Value] =
    user.ssoAccessToken.map {
      t =>
        val token = SsoAccessToken(t)
        val elevation = sso.sessionStatus(token).map(s => (s.status, s.sessionElevation)).map {
          case (SsoTokenStatus.Valid, Some(SsoTokenElevation.Critical)) => Elevation.Critical
          case (SsoTokenStatus.Valid, _) => Elevation.Unelevated
          case (s, _) => throw new InvalidTokenStatusException(s"The provided token is not valid (status: $s)")
        }
        elevation.onSuccess{
          case Elevation.Critical => sso.extendSession(token)
        }
        elevation
    }.getOrElse(Future.successful(Elevation.Unelevated))
}
