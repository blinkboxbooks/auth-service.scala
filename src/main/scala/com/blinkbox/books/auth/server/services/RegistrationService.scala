package com.blinkbox.books.auth.server.services

import java.sql.{DataTruncation, SQLException}

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{ClientRegistered, Publisher, UserRegistered}
import com.blinkbox.books.auth.server.sso.{SSO, TokenCredentials}
import com.blinkbox.books.slick.DatabaseTypes
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait RegistrationService {
  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo]
}

class DefaultRegistrationService[DbTypes <: DatabaseTypes](
    db: DbTypes#Database,
    authRepo: AuthRepository[DbTypes#Profile],
    userRepo: UserRepository[DbTypes#Profile],
    clientRepo: ClientRepository[DbTypes#Profile],
    geoIP: GeoIP,
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock, tag: ClassTag[DbTypes#ConstraintException]) extends RegistrationService {

  // TODO: Make this configurable
  private val TermsAndConditionsVersion = "1.0"

  private def validateRegistration(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[UserRegistration] =
    Future {
      if (!registration.acceptedTerms)
        Future.failed(Failures.termsAndConditionsNotAccepted)

      else if (clientIp.isDefined && clientIp.map(geoIP.countryCode).filter(s => s == "GB" || s == "IE").isEmpty)
        Future.failed(Failures.notInTheUK)

      registration
    }

  // TODO: Add persistence for token credentials
  private def persistDetails(registration: UserRegistration, credentials: TokenCredentials): Future[(User, Option[Client], RefreshToken)] =
    Future {
      db.withTransaction { implicit transaction =>
        val u = userRepo.createUser(registration)
        val c = registration.client.map(clientRepo.createClient(u.id, _))
        val t = authRepo.createRefreshToken(u.id, c.map(_.id))
        (u, c, t)
      }
    }

  private val errorTransformer = (_: Throwable) match {
    case e: DataTruncation => Failures.requestException(e.getMessage, InvalidRequest)
    case e: DbTypes#ConstraintException => Failures.usernameAlreadyTaken
    case e => e
  }

  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo] = {
    val tokenInfo = for {
      reg                   <- validateRegistration(registration, clientIp)
      cred                  <- sso register reg
      (user, client, token) <- persistDetails(reg, cred)
      _                     <- sso linkAccount(user.id, registration.allowMarketing, TermsAndConditionsVersion)
      _                     <- events publish UserRegistered(user)
      _                     <- client map(cl => events publish ClientRegistered(cl)) getOrElse(Future.successful(()))
    } yield TokenBuilder.issueAccessToken(user, client, token, includeRefreshToken = true, includeClientSecret = true)

    tokenInfo.transform(identity, errorTransformer)
  }
}
