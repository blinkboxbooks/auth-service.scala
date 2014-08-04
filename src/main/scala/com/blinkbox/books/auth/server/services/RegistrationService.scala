package com.blinkbox.books.auth.server.services

import java.sql.{SQLException, DataTruncation}

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.ZuulRequestErrorReason.UsernameAlreadyTaken
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{ClientRegistered, UserRegistered, Publisher}
import com.blinkbox.books.auth.server.sso.{TokenCredentials, SSO}
import com.blinkbox.books.auth.server._
import com.blinkbox.books.time.Clock
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait RegistrationService {
  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo]
}

class DefaultRegistrationService[Profile <: BasicProfile, Database <: Profile#Backend#Database](
    db: Database,
    authRepo: AuthRepository[Profile],
    userRepo: UserRepository[Profile],
    clientRepo: ClientRepository[Profile],
    geoIP: GeoIP,
    events: Publisher,
    sso: SSO)(implicit executionContext: ExecutionContext, clock: Clock) {

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
    case e: DataTruncation => ZuulRequestException(e.getMessage, InvalidRequest)
    case e: SQLException => ZuulRequestException(e.getMessage, InvalidRequest, Some(UsernameAlreadyTaken))
    case e => e
  }

  def registerUser(registration: UserRegistration, clientIp: Option[RemoteAddress]): Future[TokenInfo] = {
    val tokenInfo = for {
      reg                   <- validateRegistration(registration, clientIp)
      cred                  <- sso register reg
      (user, client, token) <- persistDetails(reg, cred)
      _                     <- sso linkAccount(user.id, registration.allowMarketing, TermsAndConditionsVersion)
      _                     <- events publish UserRegistered(user)
      _                     <- client.map(cl => events publish ClientRegistered(cl)).getOrElse(Future.successful(()))
    } yield TokenBuilder.issueAccessToken(user, client, token, includeRefreshToken = true, includeClientSecret = true)

    tokenInfo.transform(identity, errorTransformer)
  }
}
