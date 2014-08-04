package com.blinkbox.books.auth.server.services

import java.nio.file.{Files, Paths}
import java.security.KeyFactory
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.sql.{DataTruncation, SQLException}

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.ZuulRequestErrorReason.UsernameAlreadyTaken
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events._
import com.blinkbox.books.auth.server.sso.SSO
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock
import com.blinkbox.security.jwt.TokenEncoder
import com.blinkbox.security.jwt.encryption.{A128GCM, RSA_OAEP}
import com.blinkbox.security.jwt.signatures.ES256
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait AuthService {
  def revokeRefreshToken(token: String): Future[Unit]
  def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo]
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

  // TODO: Make these configurable
  val MaxClients = 12
  val PrivateKeyPath = "/opt/bbb/keys/blinkbox/zuul/sig/ec/1/private.key"

  override def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    if (!registration.acceptedTerms)
      FailWith.termsAndConditionsNotAccepted

    if (registration.password.length < 6)
      FailWith.passwordTooShort

    if (clientIP.isDefined && clientIP.map(geoIP.countryCode).filter(s => s == "GB" || s == "IE").isEmpty)
      FailWith.notInTheUK

    val (user, client, token) = db.withTransaction { implicit transaction =>
      val u = userRepo.createUser(registration)
      val c = registration.client.map(clientRepo.createClient(u.id, _))
      val t = authRepo.createRefreshToken(u.id, c.map(_.id))
      (u, c, t)
    }
    events.publish(UserRegistered(user))
    client.foreach(c => events.publish(ClientRegistered(c)))
    issueAccessToken(user, client, token, includeRefreshToken = true, includeClientSecret = true)
  }.transform(identity, _ match {
    case e: DataTruncation => ZuulRequestException(e.getMessage, InvalidRequest)
    case e: SQLException => ZuulRequestException(e.getMessage, InvalidRequest, Some(UsernameAlreadyTaken))
    case e => e
  })

  override def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    val (user, client, token) = db.withTransaction { implicit transaction =>
      val u = authenticateUser(credentials, clientIP)
      val c = authenticateClient(credentials, u)
      val t = authRepo.createRefreshToken(u.id, c.map(_.id))
      (u, c, t)
    }
    events.publish(UserAuthenticated(user, client))
    issueAccessToken(user, client, token, includeRefreshToken = true)
  }

  override def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = Future {
    val (user1, client1, token1) = db.withTransaction { implicit transaction =>
      val t = authRepo.refreshTokenWithToken(credentials.token).getOrElse(FailWith.invalidRefreshToken)
      val u = userRepo.userWithId(t.userId).getOrElse(FailWith.invalidRefreshToken)
      val c = authenticateClient(credentials, u)

      (t.clientId, c) match {
        case (None, Some(client)) => authRepo.associateRefreshTokenWithClient(t, client) // Token needs to be associated with the client
        case (None, None) => // Do nothing: token isn't associated with a client and there is no client
        case (Some(tId), Some(client)) if (tId == client.id) => // Do nothing: token is associated with the right client
        case _ => FailWith.refreshTokenNotAuthorized
      }

      authRepo.extendRefreshTokenLifetime(t)
      (u, c, t)
    }
    events.publish(UserAuthenticated(user1, client1))
    issueAccessToken(user1, client1, token1)
  }

  override def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = Future {
    // TODO: This line should be re-written to avoid the `get` invocation on the option and to account for casting failure
    val tokenId = RefreshTokenId(user.claims.get("zl/rti").get.asInstanceOf[Int])

    val token = db.withSession(implicit session => authRepo.refreshTokenWithId(tokenId)).getOrElse(FailWith.unverifiedIdentity)
    SessionInfo(
      token_status = token.status,
      token_elevation = if (token.isValid) Some(token.elevation) else None,
      token_elevation_expires_in = if (token.isValid) Some(token.elevationDropsIn.toSeconds) else None
      // TODO: Roles
    )
  }

  override def revokeRefreshToken(token: String): Future[Unit] = Future {
    db.withSession { implicit session =>
      val retrievedToken = authRepo.refreshTokenWithToken(token).getOrElse(FailWith.invalidRefreshToken)
      authRepo.revokeRefreshToken(retrievedToken)
    }
  }

  private def authenticateUser(credentials: PasswordCredentials, clientIP: Option[RemoteAddress])(implicit session: authRepo.Session): User = {
    val user = userRepo.userWithUsernameAndPassword(credentials.username, credentials.password)
    authRepo.recordLoginAttempt(credentials.username, user.isDefined, clientIP)

    user.getOrElse(FailWith.invalidUsernamePassword)
  }

  private def authenticateClient(credentials: ClientCredentials, user: User)(implicit session: authRepo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield authRepo.
      authenticateClient(clientId, clientSecret, user.id).
      getOrElse(FailWith.invalidClientCredentials)

  private def buildAccessToken(user: User, client: Option[Client], token: RefreshToken, expiresAt: DateTime) = {

    // TODO: Do this properly with configurable keys etc.

    val claims = new java.util.LinkedHashMap[String, AnyRef]
    claims.put("sub", s"urn:blinkbox:zuul:user:${user.id.value}")
    claims.put("exp", Long.box(expiresAt.getMillis))
    client.foreach(c => claims.put("bb/cid", s"urn:blinkbox:zuul:client:${c.id.value}"))
    // TODO: Roles
    claims.put("zl/rti", Int.box(token.id.value))

    val signingKeyData = Files.readAllBytes(Paths.get(PrivateKeyPath))
    val signingKeySpec = new PKCS8EncodedKeySpec(signingKeyData)
    val signingKey = KeyFactory.getInstance("EC").generatePrivate(signingKeySpec)
    val signer = new ES256(signingKey)
    val signingHeaders = new java.util.LinkedHashMap[String, AnyRef]
    signingHeaders.put("kid", "/blinkbox/zuul/sig/ec/1")

    val encryptionKeyData = Files.readAllBytes(Paths.get("/opt/bbb/keys/blinkbox/plat/enc/rsa/1/public.key"))
    val encryptionKeySpec = new X509EncodedKeySpec(encryptionKeyData)
    val encryptionKey = KeyFactory.getInstance("RSA").generatePublic(encryptionKeySpec)
    val encryptionAlgorithm = new RSA_OAEP(encryptionKey)
    val encrypter = new A128GCM(encryptionAlgorithm)
    val encryptionHeaders = new java.util.LinkedHashMap[String, AnyRef]
    encryptionHeaders.put("kid", "/blinkbox/plat/enc/rsa/1")
    encryptionHeaders.put("cty", "JWT")

    val encoder = new TokenEncoder()
    val signed = encoder.encode(claims, signer, signingHeaders)
    val encrypted = encoder.encode(signed, encrypter, encryptionHeaders)

    encrypted
  }

  private def issueAccessToken(user: User, client: Option[Client], token: RefreshToken, includeRefreshToken: Boolean = false, includeClientSecret: Boolean = false): TokenInfo = {
    val expiresAt = DateTime.now(DateTimeZone.UTC).plusSeconds(1800)
    TokenInfo(
      access_token = buildAccessToken(user, client, token, expiresAt),
      token_type = "bearer",
      expires_in = 1800,
      refresh_token = if (includeRefreshToken) Some(token.token) else None,
      user_id = s"urn:blinkbox:zuul:user:${user.id.value}",
      user_uri = s"/users/${user.id.value}",
      user_username = user.username,
      user_first_name = user.firstName,
      user_last_name = user.lastName,
      client_id = client.map(row => s"urn:blinkbox:zuul:client:${row.id.value}"),
      client_uri = client.map(row => s"/clients/${row.id.value}"),
      client_name = client.map(_.name),
      client_brand = client.map(_.brand),
      client_model = client.map(_.model),
      client_os = client.map(_.os),
      client_secret = if (includeClientSecret) client.map(_.secret) else None,
      last_used_date = client.map(_.updatedAt))
  }
}
