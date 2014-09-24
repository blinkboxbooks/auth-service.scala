package com.blinkbox.books.auth.server.services

import java.io.File

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.maxmind.geoip2.DatabaseReader.Builder
import com.maxmind.geoip2.exception.GeoIp2Exception
import spray.http.RemoteAddress

import scala.slick.profile.BasicProfile
import scala.util.{Failure, Success, Try}

trait ClientAuthenticator[Profile <: BasicProfile] {
  protected def authenticateClient(
      authRepo: AuthRepository[Profile],
      credentials: ClientCredentials,
      userId: UserId)(implicit session: authRepo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield authRepo.
      authenticateClient(clientId, clientSecret, userId).
      getOrElse(throw Failures.invalidClientCredentials)
}

trait UserInfoFactory {
  def userInfoFromUser(user: User) = UserInfo(
    user_id = user.id.external,
    user_uri = user.id.uri,
    user_username = user.username,
    user_first_name = user.firstName,
    user_last_name = user.lastName,
    user_allow_marketing_communications = user.allowMarketing
  )
}

trait ClientInfoFactory {
  def clientInfo(client: Client, includeClientSecret: Boolean = false) = ClientInfo(
    client_id = client.id.external,
    client_uri = client.id.uri,
    client_name = client.name,
    client_brand = client.brand,
    client_model = client.model,
    client_os = client.os,
    client_secret = if (includeClientSecret) Some(client.secret) else None,
    last_used_date = client.createdAt.toLocalDate)
}

trait GeoIP {
  def countryCode(address: RemoteAddress): Option[String]
}

class MaxMindGeoIP extends GeoIP {
  private val geoIpDb = new File(getClass.getResource("/geoip/GeoIP2-Country.mmdb").toURI.getPath)

  require(geoIpDb.exists(), "Cannot find GeoIP database")

  private val db = new Builder(geoIpDb).build()

  override def countryCode(address: RemoteAddress): Option[String] = address.toOption.flatMap { a =>
    Try(db.country(a).getCountry.getIsoCode) match {
      case Success(r) => Some(r)
      case Failure(ex: GeoIp2Exception) => None
      case Failure(ex) => throw ex
    }
  }
}
