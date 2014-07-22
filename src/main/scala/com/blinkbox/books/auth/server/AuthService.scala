package com.blinkbox.books.auth.server

import java.nio.file.{Files, Paths}
import java.security.KeyFactory
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.ZuulRequestErrorReason.UsernameAlreadyTaken
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.time.Clock
import com.blinkbox.security.jwt.TokenEncoder
import com.blinkbox.security.jwt.encryption.{A128GCM, RSA_OAEP}
import com.blinkbox.security.jwt.signatures.ES256
import com.mysql.jdbc.MysqlDataTruncation
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}

trait AuthService {
  def updateUser(id: Int, patch: UserPatch): Future[Option[UserInfo]]
  def getUserInfo(implicit user: AuthenticatedUser): Future[Option[UserInfo]]
  def revokeRefreshToken(token: String): Future[Unit]
  def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo]
  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]
  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]
  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo]
  def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser): Future[ClientInfo]
  def listClients()(implicit user: AuthenticatedUser): Future[ClientList]
  def getClientById(id: Int)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def updateClient(id: Int, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def deleteClient(id: Int)(implicit user: AuthenticatedUser): Future[Unit]
}

trait GeoIP {
  def countryCode(address: RemoteAddress): String
}

class DefaultAuthService(repo: AuthRepository, geoIP: GeoIP, events: Publisher)(implicit executionContext: ExecutionContext, clock: Clock) extends AuthService {
  // TODO: Make these configurable
  val MaxClients = 12
  val PrivateKeyPath = "/opt/bbb/keys/blinkbox/zuul/sig/ec/1/private.key"

  def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    if (!registration.acceptedTerms)
      FailWith.termsAndConditionsNotAccepted

    if (registration.password.length < 6)
      FailWith.passwordTooShort

    if (clientIP.isDefined && clientIP.map(geoIP.countryCode).filter(s => s == "GB" || s == "IE").isEmpty)
      FailWith.notInTheUK

    val (user, client, token) = repo.db.withTransaction { implicit transaction =>
      val u = repo.createUser(registration)
      val c = registration.client.map(repo.createClient(u.id, _))
      val t = repo.createRefreshToken(u.id, c.map(_.id))
      (u, c, t)
    }
    events.userRegistered(user, client)
    issueAccessToken(user, client, token, includeRefreshToken = true, includeClientSecret = true)
  }.transform(identity, _ match {
    case e: MysqlDataTruncation => ZuulRequestException(e.getMessage, InvalidRequest)
    case e: MySQLIntegrityConstraintViolationException => ZuulRequestException(e.getMessage, InvalidRequest, Some(UsernameAlreadyTaken))
    case e => e
  })

  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    val (user, client, token) = repo.db.withTransaction { implicit transaction =>
      val u = authenticateUser(credentials, clientIP)
      val c = authenticateClient(credentials, u)
      val t = repo.createRefreshToken(u.id, c.map(_.id))
      (u, c, t)
    }
    events.userAuthenticated(user, client)
    issueAccessToken(user, client, token, includeRefreshToken = true)
  }

  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = Future {
    val (user1, client1, token1) = repo.db.withTransaction { implicit transaction =>
      val t = repo.refreshTokenWithToken(credentials.token).getOrElse(FailWith.invalidRefreshToken)
      val u = repo.userWithId(t.userId).getOrElse(FailWith.invalidRefreshToken)
      val c = authenticateClient(credentials, u)

      (t.clientId, c) match {
        case (None, Some(client)) => repo.associateRefreshTokenWithClient(t, client) // Token needs to be associated with the client
        case (None, None) => // Do nothing: token isn't associated with a client and there is no client
        case (Some(tId), Some(client)) if (tId == client.id) => // Do nothing: token is associated with the right client
        case _ => FailWith.refreshTokenNotAuthorized
      }

      repo.extendRefreshTokenLifetime(t)
      (u, c, t)
    }
    events.userAuthenticated(user1, client1)
    issueAccessToken(user1, client1, token1)
  }

  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = Future {
    val tokenId = user.claims.get("zl/rti").get.asInstanceOf[Int]
    val token = repo.db.withSession(implicit session => repo.refreshTokenWithId(tokenId)).getOrElse(FailWith.unverifiedIdentity)
    SessionInfo(
      token_status = token.status,
      token_elevation = if (token.isValid) Some(token.elevation) else None,
      token_elevation_expires_in = if (token.isValid) Some(token.elevationDropsIn.toSeconds) else None
      // TODO: Roles
    )
  }

  def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser): Future[ClientInfo] = Future {
    val client = repo.db.withTransaction { implicit transaction =>
      if (repo.activeClientCount >= MaxClients) {
        FailWith.clientLimitReached
      }
      repo.createClient(user.id, registration)
    }
    events.clientRegistered(client)
    clientInfo(client, includeClientSecret = true)
  } transform(identity, _ match {
    case e: MysqlDataTruncation => ZuulRequestException(e.getMessage, InvalidRequest)
    case e => e
  })

  def listClients()(implicit user: AuthenticatedUser): Future[ClientList] = Future {
    val clientList = repo.db.withSession(implicit session => repo.activeClients)
    ClientList(clientList.map(clientInfo(_)))
  }

  def getClientById(id: Int)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val client = repo.db.withSession(implicit session => repo.clientWithId(id))
    client.map(clientInfo(_))
  }

  def updateClient(id: Int, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val clients = repo.db.withTransaction { implicit transaction =>
      val clientPair = repo.clientWithId(id) map { oldClient =>
        val newClient = oldClient.copy(
          updatedAt = clock.now(),
          name = patch.client_name.getOrElse(oldClient.name),
          brand = patch.client_brand.getOrElse(oldClient.brand),
          model = patch.client_model.getOrElse(oldClient.model),
          os = patch.client_os.getOrElse(oldClient.os))
        (oldClient, newClient)
      }
      clientPair foreach { case (_, c) => repo.updateClient(c) }
      clientPair
    }

    clients foreach { case (o, n) => events.clientUpdated(o, n) }
    clients map { case (_, c) => clientInfo(c) }
  }

  def deleteClient(id: Int)(implicit user: AuthenticatedUser): Future[Unit] = Future {
    val client = repo.db.withSession { implicit session =>
      val newClient = repo.clientWithId(id).map(_.copy(updatedAt = clock.now(), isDeregistered = true))
      newClient.foreach(repo.updateClient)
      newClient
    }
    client.foreach(events.clientDeregistered)
  }

  private def authenticateUser(credentials: PasswordCredentials, clientIP: Option[RemoteAddress])(implicit session: repo.Session): User = {
    val user = repo.authenticateUser(credentials.username, credentials.password)
    repo.recordLoginAttempt(credentials.username, user.isDefined, clientIP)

    user.getOrElse(FailWith.invalidUsernamePassword)
  }

  private def authenticateClient(credentials: ClientCredentials, user: User)(implicit session: repo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield repo.
      authenticateClient(clientId, clientSecret, user.id).
      getOrElse(FailWith.invalidClientCredentials)

  private def clientInfo(client: Client, includeClientSecret: Boolean = false) = ClientInfo(
    client_id = s"urn:blinkbox:zuul:client:${client.id}",
    client_uri = s"/clients/${client.id}",
    client_name = client.name,
    client_brand = client.brand,
    client_model = client.model,
    client_os = client.os,
    client_secret = if (includeClientSecret) Some(client.secret) else None,
    last_used_date = client.createdAt)

  private def buildAccessToken(user: User, client: Option[Client], token: RefreshToken, expiresAt: DateTime) = {

    // TODO: Do this properly with configurable keys etc.

    val claims = new java.util.LinkedHashMap[String, AnyRef]
    claims.put("sub", s"urn:blinkbox:zuul:user:${user.id}")
    claims.put("exp", Long.box(expiresAt.getMillis))
    client.foreach(c => claims.put("bb/cid", s"urn:blinkbox:zuul:client:${c.id}"))
    // TODO: Roles
    claims.put("zl/rti", Int.box(token.id))

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
      user_id = s"urn:blinkbox:zuul:user:${user.id}",
      user_uri = s"/users/${user.id}",
      user_username = user.username,
      user_first_name = user.firstName,
      user_last_name = user.lastName,
      client_id = client.map(row => s"urn:blinkbox:zuul:client:${row.id}"),
      client_uri = client.map(row => s"/clients/${row.id}"),
      client_name = client.map(_.name),
      client_brand = client.map(_.brand),
      client_model = client.map(_.model),
      client_os = client.map(_.os),
      client_secret = if (includeClientSecret) client.map(_.secret) else None,
      last_used_date = client.map(_.updatedAt))
  }

  override def revokeRefreshToken(token: String): Future[Unit] = Future {
    repo.db.withSession { implicit session =>
      val retrievedToken = repo.refreshTokenWithToken(token).getOrElse(FailWith.invalidRefreshToken)
      repo.revokeRefreshToken(retrievedToken)
    }
  }

  private def userInfoFromUser(user: User) = UserInfo(
    user_id = s"urn:blinkbox:zuul:user:${user.id}",
    user_uri = s"/users/${user.id}",
    user_username = user.username,
    user_first_name = user.firstName,
    user_last_name = user.lastName,
    user_allow_marketing_communications = user.allowMarketing
  )

  override def getUserInfo(implicit user: AuthenticatedUser): Future[Option[UserInfo]] = Future {
    repo.db.withSession { implicit session =>
      repo.userWithId(user.id).map(userInfoFromUser)
    }
  }

  override def updateUser(id: Int, patch: UserPatch): Future[Option[UserInfo]] = Future {
    repo.db.withSession { implicit session =>
      val updatedUser = repo.userWithId(id).map { user =>
        user.copy(
          firstName = patch.first_name.getOrElse(user.firstName),
          lastName = patch.last_name.getOrElse(user.lastName),
          username = patch.username.getOrElse(user.username),
          allowMarketing = patch.allow_marketing_communications.getOrElse(user.allowMarketing)
        )
      }

      updatedUser foreach repo.updateUser

      updatedUser map userInfoFromUser
    }
  }
}
