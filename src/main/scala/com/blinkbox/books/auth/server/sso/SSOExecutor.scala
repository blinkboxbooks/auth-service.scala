package com.blinkbox.books.auth.server.sso

import org.json4s.DefaultFormats
import scala.concurrent.Future
import spray.client.pipelining._
import spray.http._
import spray.httpx.Json4sJacksonSupport

/**
 * Trait to be extended to make a Req type part of the SSOExecutor typeclass
 */
trait SSOExecutor[-Req, +Resp] {
  def apply(r: Req): Future[Resp]
}

object SSOExecutor {
  def apply[Req, Resp](f: Req => Future[Resp]) = new SSOExecutor[Req, Resp] { def apply(r: Req) = f(r) }
}

object DefaultSSOConstants {
  val TokenUri = "/oauth2/token"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
}

/**
 * Container for the default implementations of the SSOExecutor typeclass
 */
class DefaultSSOExecutors(config: SSOConfig, client: Client) extends Json4sJacksonSupport {
  implicit val json4sJacksonFormats = DefaultFormats

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = DefaultSSOConstants

  implicit val registration: SSOExecutor[RegisterUser, TokenCredentials] = SSOExecutor { req: RegisterUser =>
    client.dataRequest[TokenCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "username" -> req.username,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "service_user_id" -> req.id.value.toString,
      "service_allow_marketing" -> (if (req.allowMarketing) "true" else "false"),
      "service_tc_accepted_version" -> req.acceptedTermsVersion
    ))))
  }
}
