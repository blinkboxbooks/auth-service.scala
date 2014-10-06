package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.{Elevation, ElevationChecker, User}

import scala.concurrent.{ExecutionContext, Future}

class SsoElevationChecker(sso: Sso)(implicit executionContext: ExecutionContext) extends ElevationChecker {
  override def apply(user: User) =
    user.ssoAccessToken.map{
      t =>
        val token = SsoAccessToken(t)
        val elevation = sso.sessionStatus(token).map(_.sessionElevation).map {
          case Some(SsoTokenElevation.Critical) => Elevation.Critical
          case _ => Elevation.Unelevated
        }
        elevation.onSuccess{
          case Elevation.Critical => sso.extendSession(token)
        }
        elevation
    }.getOrElse(Future.successful(Elevation.Unelevated))
}
