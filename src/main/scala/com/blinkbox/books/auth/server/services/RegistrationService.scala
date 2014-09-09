package com.blinkbox.books.auth.server.services

import java.sql.DataTruncation

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data.{Client => UserClient}
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{ClientRegistered, Publisher, UserRegistered}
import com.blinkbox.books.auth.server.sso._
import com.blinkbox.books.slick.{UnknownDatabaseException, ConstraintException, DatabaseSupport}
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}

trait RegistrationService {
  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo]
}

class DefaultRegistrationService[DB <: DatabaseSupport](
    db: DB#Database,
    authRepo: AuthRepository[DB#Profile],
    userRepo: UserRepository[DB#Profile],
    clientRepo: ClientRepository[DB#Profile],
    exceptionFilter: DB#ExceptionFilter,
    termsVersion: String,
    tokenBuilder: TokenBuilder,
    geoIP: GeoIP,
    events: Publisher,
    sso: Sso)(implicit executionContext: ExecutionContext, clock: Clock) extends RegistrationService {

  private def validateRegistration(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[UserRegistration] =
      if (!registration.acceptedTerms)
        Future.failed(Failures.termsAndConditionsNotAccepted)
      else if (clientIp.isDefined && clientIp.flatMap(geoIP.countryCode).filter(s => s == "GB" || s == "IE").isEmpty)
        Future.failed(Failures.notInTheUK)
      else Future.successful(registration)

  private def persistDetails(registration: UserRegistration, credentials: SsoCredentials): Future[(User, Option[UserClient], RefreshToken)] =
    Future {
      db.withTransaction { implicit transaction =>
        val u = userRepo.createUser(registration)
        val c = registration.client.map(clientRepo.createClient(u.id, _))
        val t = authRepo.createRefreshToken(u.id, c.map(_.id), credentials.refreshToken)
        (u, c, t)
      }
    }

  private def markLinked(user: User, ssoId: SsoUserId): Future[Unit] = Future {
    db.withSession { implicit session =>
      userRepo.updateUser(user.copy(ssoId = Some(ssoId)))
    }
  }

  private val errorTransformer = exceptionFilter {
    case ConstraintException(e) => Failures.unknownError("Unexpected constraint error", Some(e))
    case UnknownDatabaseException(e) => Failures.requestException(e.getMessage, InvalidRequest)
    case SsoConflict => Failures.usernameAlreadyTaken
    case SsoInvalidRequest(msg) => Failures.requestException(msg, InvalidRequest)
    case e => e
  }

  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo] = {
    val tokenInfo = for {
      reg                   <- validateRegistration(registration, clientIp)
      (ssoId, cred)         <- sso register reg
      (user, client, token) <- persistDetails(reg, cred)
      _                     <- sso linkAccount(cred.accessToken, user.id, registration.allowMarketing, termsVersion)
      _                     <- markLinked(user, ssoId)
      _                     <- events publish UserRegistered(user)
      _                     <- client map(cl => events publish ClientRegistered(user, cl)) getOrElse(Future.successful(()))
    } yield tokenBuilder.issueAccessToken(user, client, token, Some(cred), includeRefreshToken = true, includeClientSecret = true)

    tokenInfo.transform(identity, errorTransformer)
  }
}
