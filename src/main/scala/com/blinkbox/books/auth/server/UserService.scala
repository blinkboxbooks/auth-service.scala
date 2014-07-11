package com.blinkbox.books.auth.server

import java.nio.file.{Files, Paths}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, SecureRandom}
import java.util.concurrent.TimeUnit

import com.blinkbox.books.auth.{Elevation, User => AuthenticatedUser}
import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.spray._
import com.blinkbox.security.jwt.TokenEncoder
import com.blinkbox.security.jwt.encryption.{A128GCM, RSA_OAEP}
import com.blinkbox.security.jwt.signatures.ES256
import com.blinkbox.security.jwt.util.Base64
import com.lambdaworks.crypto.SCryptUtil
import com.mysql.jdbc.MysqlDataTruncation
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.RemoteAddress

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver.simple._

import com.blinkbox.books.auth.server.OAuthClientErrorCode._
import com.blinkbox.books.auth.server.OAuthClientErrorReason._
import com.blinkbox.books.auth.server.OAuthServerErrorCode._
import com.blinkbox.books.auth.server.OAuthServerErrorReason._

trait UserService {
  def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo]
  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]
  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]
  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo]
  def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser): Future[ClientInfo]
  def listClients()(implicit user: AuthenticatedUser): Future[ClientList]
  def getClientById(id: Int)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def updateClient(id: Int, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def deleteClient(id: Int)(implicit user: AuthenticatedUser): Future[Unit]
//  def list(page: Page)(implicit user: User): Future[ListPage[User]]
//  def getById(id: Int)(implicit user: User): Future[Option[User]]
//  def update(id: Int, patch: UserPatch)(implicit user: User): Future[Option[User]]
//  def delete(id: Int)(implicit user: User): Future[Unit]
}


object DataModel {

  case class User(id: Int, createdAt: DateTime, updatedAt: DateTime, username: String, firstName: String, lastName: String, passwordHash: String, allowMarketing: Boolean)

  case class Client(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean)

  case class RefreshToken(id: Int, createdAt: DateTime, updatedAt: DateTime, userId: Int, clientId: Option[Int], token: String, isRevoked: Boolean, expiresAt: DateTime, elevationExpiresAt: DateTime, criticalElevationExpiresAt: DateTime) {
    def isExpired = expiresAt.isBeforeNow
    def isValid = !isExpired && !isRevoked
    def status = if (isValid) RefreshTokenStatus.Valid else RefreshTokenStatus.Invalid
    def isElevated = !elevationExpiresAt.isBeforeNow
    def isCriticallyElevated = !criticalElevationExpiresAt.isBeforeNow
    def elevation = if (isCriticallyElevated) Elevation.Critical
                    else if (isElevated) Elevation.Elevated
                    else Elevation.Unelevated
    def elevationDropsAt = if (isCriticallyElevated) criticalElevationExpiresAt else elevationExpiresAt
    def elevationDropsIn = FiniteDuration(elevationDropsAt.getMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
  }

  case class LoginAttempt(createdAt: DateTime, username: String, successful: Boolean, clientIP: String)

  implicit def dateTime = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime))

  val users = TableQuery[Users]
  val clients = TableQuery[Clients]
  val refreshTokens = TableQuery[RefreshTokens]
  val loginAttempts = TableQuery[LoginAttempts]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def firstName = column[String]("first_name", O.NotNull)
    def lastName = column[String]("last_name", O.NotNull)
    def passwordHash = column[String]("password_hash", O.NotNull)
    def allowMarketing = column[Boolean]("allow_marketing_communications", O.NotNull)
    def * = (id, createdAt, updatedAt, username, firstName, lastName, passwordHash, allowMarketing) <> ((User.apply _).tupled, User.unapply)
    def indexOnUsername = index("index_users_on_username", username, unique = true)
  }

  class Clients(tag: Tag) extends Table[Client](tag, "clients") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def name = column[String]("name", O.NotNull)
    def brand = column[String]("brand", O.NotNull)
    def model = column[String]("model", O.NotNull)
    def os = column[String]("os", O.NotNull)
    def secret = column[String]("client_secret", O.NotNull)
    def isDeregistered = column[Boolean]("deregistered", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, name, brand, model, os, secret, isDeregistered) <> ((Client.apply _).tupled, Client.unapply)
    def indexOnUserId = index("index_users_on_username", userId)
  }

  class RefreshTokens(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def clientId = column[Option[Int]]("client_id")
    def token = column[String]("token", O.NotNull)
    def isRevoked = column[Boolean]("revoked", O.NotNull)
    def expiresAt = column[DateTime]("expires_at", O.NotNull)
    def elevationExpiresAt = column[DateTime]("elevation_expires_at", O.NotNull)
    def criticalElevationExpiresAt = column[DateTime]("critical_elevation_expires_at", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, clientId, token, isRevoked, expiresAt, elevationExpiresAt, criticalElevationExpiresAt) <> ((RefreshToken.apply _).tupled, RefreshToken.unapply)
    def indexOnToken = index("index_refresh_tokens_on_token", token)
    def user = foreignKey("fk_refresh_tokens_to_users", userId, users)(_.id)
  }

  class LoginAttempts(tag: Tag) extends Table[LoginAttempt](tag, "login_attempts") {
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def successful = column[Boolean]("successful", O.NotNull)
    def clientIP = column[String]("client_ip", O.NotNull)
    def * = (createdAt, username, successful, clientIP) <> ((LoginAttempt.apply _).tupled, LoginAttempt.unapply)
  }

  object LoginAttempt {
    def newAttempt(username: String, successful: Boolean, clientIP: String) = LoginAttempt(DateTime.now(DateTimeZone.UTC), username, successful, clientIP)
  }

  object User {

    def create(registration: UserRegistration)(implicit session: Session): User = {
      val user = User.forInsert(registration)
      val id = (users returning users.map(_.id)) += user
      user.copy(id = id)
    }

    def authenticate(username: String, password: String, clientIP: Option[RemoteAddress])(implicit session: Session): User = {
      val user = users.where(_.username === username).firstOption
      val passwordValid = if (user.isDefined) {
        SCryptUtil.check(password, user.get.passwordHash)
      } else {
        // even if the user isn't found we still need to perform an scrypt hash of something to help
        // prevent timing attacks as this hashing process is the bulk of the request time
        hashPassword("random string")
        false
      }
      loginAttempts += LoginAttempt.newAttempt(username, passwordValid, clientIP.fold("unknown")(_.toString()))
      if (!passwordValid) throw new OAuthServerException("The username and/or password is incorrect.", InvalidGrant)
      user.get
    }

    private def forInsert(r: UserRegistration) = {
      val now = DateTime.now(DateTimeZone.UTC)
      val passwordHash = hashPassword(r.password)
      User(-1, now, now, r.username, r.firstName, r.lastName, passwordHash, r.allowMarketing)
    }

    private def hashPassword(password: String) = SCryptUtil.scrypt(password, 16384, 8, 1)
  }

  object Client {
    val ClientId = """urn:blinkbox:zuul:client:([0-9]+)""".r

    def create(userId: Int, registration: ClientRegistration)(implicit session: Session): Client = {
      val client = Client.forInsert(userId, registration)
      val id = (clients returning clients.map(_.id)) += client
      client.copy(id = id)
    }

    def authenticate(id: Option[String], secret: Option[String], userId: Int)(implicit session: Session): Option[Client] = {
      if (id.isEmpty || secret.isEmpty) return None

      val numericId = id.get match {
        case ClientId(n) => try Some(n.toInt) catch { case _: NumberFormatException => None }
        case _ => None
      }

      val client = numericId.flatMap(nid => clients.where(c => c.id === nid && c.secret === secret).firstOption)
      if (client.isEmpty) throw new OAuthServerException("The client id and/or client secret is incorrect.", InvalidClient)
      if (client.get.userId != userId) throw new OAuthServerException("You are not authorised to use this client.", InvalidClient)

      clients.where(_.id === client.get.id).map(_.updatedAt).update(DateTime.now(DateTimeZone.UTC))
      client
    }

    def forInsert(userId: Int, r: ClientRegistration) = {
      val now = DateTime.now(DateTimeZone.UTC)
      val buf = new Array[Byte](32)
      new SecureRandom().nextBytes(buf)
      val secret = Base64.encode(buf)
      Client(-1, now, now, userId, r.name, r.brand, r.model, r.os, secret, false)
    }
  }

  object RefreshToken {

    def create(userId: Int, clientId: Option[Int])(implicit session: Session): RefreshToken = {
      val token = RefreshToken.forInsert(userId, clientId)
      val id = (refreshTokens returning refreshTokens.map(_.id)) += token
      token.copy(id = id)
    }

    private def forInsert(userId: Int, clientId: Option[Int]) = {
      val now = DateTime.now(DateTimeZone.UTC)
      val buf = new Array[Byte](32)
      new SecureRandom().nextBytes(buf)
      val token = Base64.encode(buf)
      RefreshToken(-1, now, now, userId, clientId, token, false, now.plusDays(90), now.plusHours(24), now.plusMinutes(10))
    }
  }
}

trait Clock { def now() = DateTime.now(DateTimeZone.UTC) }
object SystemClock extends Clock

class DefaultUserService(config: DatabaseConfig, clock: Clock)(implicit executionContext: ExecutionContext) extends UserService {
  val db = Database.forURL("jdbc:" + config.uri.withUserInfo("").toString, driver="com.mysql.jdbc.Driver", user = "zuul", password = "mypass")

  import com.blinkbox.books.auth.server.DataModel._

  def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo] = {
    if (!registration.acceptedTerms) return Future.failed(new OAuthServerException("You must accept the terms and conditions", InvalidRequest))
    if (registration.password.length < 6) return Future.failed(new OAuthServerException("password must be at least 6 characters", InvalidRequest))
    Future {
      // TODO: GeoIP checks
      val (user, client, token) = db.withTransaction { implicit transaction =>
        val u = User.create(registration)
        val c = registration.client.map(Client.create(u.id, _))
        val t = RefreshToken.create(u.id, c.map(_.id))
        (u, c, t)
      }
      // TODO: Send user registered message
      issueAccessToken(user, client, token, includeRefreshToken = true, includeClientSecret = true)
    } recoverWith {
      case e: MysqlDataTruncation => Future.failed(new OAuthServerException(e.getMessage, InvalidRequest))
      case e: MySQLIntegrityConstraintViolationException => Future.failed(new UserAlreadyExists(e.getMessage))
    }
  }

  def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo] = Future {
    val (user, client, token) = db.withTransaction { implicit transaction =>
      val u = User.authenticate(credentials.username, credentials.password, clientIP)
      val c = Client.authenticate(credentials.clientId, credentials.clientSecret, u.id)
      val t = RefreshToken.create(u.id, c.map(_.id))
      (u, c, t)
    }
    // TODO: Send user authenticated message
    issueAccessToken(user, client, token, includeRefreshToken = true)
  }

  def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo] = Future {
    val (user1, client1, token1) = db.withTransaction { implicit transaction =>
      val t = refreshTokens.where(_.token === credentials.token).list.headOption.getOrElse(
        throw new OAuthServerException("The refresh token is invalid.", InvalidGrant))
      if (t.isExpired) throw new OAuthServerException("The refresh token has expired.", InvalidGrant)
      if (t.isRevoked) throw new OAuthServerException("The refresh token has been revoked.", InvalidGrant)
      val c = Client.authenticate(credentials.clientId, credentials.clientSecret, t.userId)
      if (t.clientId.isEmpty) {
        if (c.isDefined) {
          refreshTokens.where(_.id === t.id).map(c => (c.updatedAt, c.clientId)).update(clock.now(), c.map(_.id))
        }
      } else if (c.isEmpty || t.clientId.get != c.get.id) {
        throw new OAuthServerException("Your client is not authorised to use this refresh token", InvalidClient)
      }
      // TODO: Extend refresh token lifetime
      // TODO: Send user authenticated message
      val u = users.where(_.id === t.userId).list.head
      (u, c, t)
    }
    issueAccessToken(user1, client1, token1)
  }

  def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo] = Future {
    val tokenId = user.claims.get("zl/rti").get.asInstanceOf[Int]
    val token = db.withSession { implicit session =>
      refreshTokens.where(_.id === tokenId).firstOption
    } getOrElse(throw new OAuthClientException("Access token is invalid", InvalidToken, Some(UnverifiedIdentity)))


    SessionInfo(
      token_status = token.status,
      token_elevation = if (token.isValid) Some(token.elevation) else None,
      token_elevation_expires_in = if (token.isValid) Some(token.elevationDropsIn.toSeconds) else None
      // TODO: Roles
    )
  }

  def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser): Future[ClientInfo] = Future {
    val client = db.withTransaction { implicit transaction =>
      val MaxClients = 12// TODO: Make max number of clients configurable
      if (clients.where(c => c.userId === user.id && !c.isDeregistered).length.run > MaxClients) {
        throw new OAuthServerException("Max clients ($MaxClients) already registered", InvalidRequest, Some(ClientLimitReached))
      }
      Client.create(user.id, registration)
    }
    clientInfo(client, includeClientSecret = true)
  } recoverWith {
    case e: MysqlDataTruncation => Future.failed(new OAuthServerException(e.getMessage, InvalidRequest))
  }

  def listClients()(implicit user: AuthenticatedUser): Future[ClientList] = Future {
    val clientList = db.withSession { implicit session =>
      clients.where(c => c.userId === user.id && !c.isDeregistered).list
    }
    ClientList(clientList.map(clientInfo(_)))
  }

  def getClientById(id: Int)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val client = db.withSession { implicit session =>
      clients.where(c => c.id === id && c.userId === user.id && !c.isDeregistered).list.headOption
    }
    client.map(clientInfo(_))
  }

  def updateClient(id: Int, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val client = db.withTransaction { implicit transaction =>
      val c = clients.where(c => c.id === id && c.userId === user.id && !c.isDeregistered).list.headOption
      c.map { c2 =>
        val c3 = c2.copy(
          updatedAt = clock.now(),
          name = patch.client_name.getOrElse(c2.name),
          brand = patch.client_brand.getOrElse(c2.brand),
          model = patch.client_model.getOrElse(c2.model),
          os = patch.client_os.getOrElse(c2.os))
        clients.where(_.id === c2.id).update(c3)
        c3
      }
    }
    client.map(clientInfo(_))
  }

  def deleteClient(id: Int)(implicit user: AuthenticatedUser): Future[Unit] = Future {
    db.withSession { implicit session =>
      val c = clients.where(c => c.id === id && c.userId === user.id && !c.isDeregistered).list.headOption
      c.foreach { c2 =>
        clients.where(_.id === c2.id).map(c => (c.updatedAt, c.isDeregistered)).update(clock.now(), true)
      }
    }
  }

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

    val signingKeyData = Files.readAllBytes(Paths.get("/opt/bbb/keys/blinkbox/zuul/sig/ec/1/private.key"))
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
  
  

//  def list(page: Page)(implicit user: User): Future[ListPage[User]] = Future {
//    db.withSession { implicit session =>
//      val q = users.where(_.userId === user.id)
//      val items = q.drop(page.offset).take(page.count).list
//      val numberOfResults = Query(q.length).first
//      ListPage(numberOfResults, page.offset, items.length, items.map(_.toUser))
//    }
//  }
//
//  def getById(id: Int)(implicit user: User): Future[Option[User]] = Future {
//    db.withSession { implicit session =>
//      users.where(r => r.id === id && r.userId === user.id).firstOption.map(_.toUser)
//    }
//  }
//
//  def update(id: Int, patch: UserPatch)(implicit user: User): Future[Option[User]] = Future {
//    db.withSession { implicit session =>
//      val q = users.where(r => r.id === id && r.userId === user.id)
//      q.map(_.name).update(patch.name)
//      q.firstOption.map(_.toUser)
//    }
//  }
//
//  def delete(id: Int)(implicit user: User): Future[Unit] = Future {
//    db.withSession { implicit session =>
//      users.where(r => r.id === id && r.userId === user.id).delete
//    }
//  }
}
