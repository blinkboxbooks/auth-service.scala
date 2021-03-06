package com.blinkbox.books.auth.server.env

import java.util.Date

import akka.actor.ActorSystem
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.cake._
import com.blinkbox.books.auth.server.data.{Client, _}
import com.blinkbox.books.auth.server.events.{Publisher, Event}
import com.blinkbox.books.auth.server.sso._
import com.blinkbox.books.auth.{User => AuthenticatedUser, UserRole}
import com.blinkbox.books.slick.H2DatabaseSupport
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import com.typesafe.config.ConfigFactory
import org.h2.jdbc.JdbcSQLException
import org.joda.time.Duration
import spray.http.HttpRequest

import scala.concurrent.Future
import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend.Database

class PublisherSpy extends Publisher {
  var events = List.empty[Event]

  override def publish(event: Event): Future[Unit] = {
    events ::= event
    Future.successful(())
  }
}

trait StoppedClockSupport extends TimeSupport {
  override val clock = StoppedClock()
}

trait TestAsyncComponent extends AsyncComponent {
  override val actorSystem: ActorSystem = ActorSystem("auth-server")
  override val apiExecutionContext = actorSystem.dispatcher
  override val ssoClientExecutionContext = actorSystem.dispatcher
  override val serviceExecutionContext = actorSystem.dispatcher
  override val rabbitExecutionContext = actorSystem.dispatcher
}

trait TestConfigComponent extends ConfigComponent {
  private def getResourcePath(resource: String) = Option(getClass.getResource(resource)).map(_.getFile) match {
    case Some(uri) => uri
    case None => sys.error(s"Cannot find resource: $resource")
  }

  System.setProperty("SSO_KEYSTORE", getResourcePath("/sso_public.key"))
  System.setProperty("AUTH_KEYS_PATH", getResourcePath("/auth_keys"))

  ConfigFactory.invalidateCaches()

  override val config = AppConfig.default
}

trait TestDatabaseComponent extends DatabaseComponent {
  val DB = new H2DatabaseSupport

  override val db = {
    val threadId = Thread.currentThread().getId()
    Database.forURL(s"jdbc:h2:mem:auth$threadId;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE", driver = "org.h2.Driver")
  }
  override val driver = H2Driver
  override val tables = ZuulTables[DB.Profile](driver)
}

trait TestEventsComponent extends EventsComponent {
  val publisherSpy = new PublisherSpy
  override val publisher = publisherSpy
}

trait TestSsoComponent extends SsoComponent {
  this: ConfigComponent with AsyncComponent =>

  val ssoResponse = new SsoResponseMocker

  protected val ssoClient = withSsoClientContext { implicit ec =>
    new TestSsoClient(config.sso, ssoResponse.nextResponse)
  }

  private val tokenDecoder = new SsoAccessTokenDecoder(SsoTestKeyStore) {
    override def validateExpirationTime(expirationTime: Date) = {} // allow expired token for test purposes
  }

  override val sso = withSsoClientContext { implicit ec =>
    new DefaultSso(config.sso, ssoClient, tokenDecoder)
  }
}

class TestEnv extends
    TestConfigComponent with
    TestAsyncComponent with
    StoppedClockSupport with
    TestSsoComponent with
    DefaultGeoIPComponent with
    TestEventsComponent with
    TestDatabaseComponent with
    DefaultTokenBuilderComponent with
    DefaultRepositoriesComponent with
    DefaultServicesComponent with
    DefaultApiComponent with
    SsoResponder {

  implicit val ec = actorSystem.dispatcher

  import driver.simple._

  def now = clock.now()

  val userIdA = UserId(1)
  val userIdB = UserId(2)
  val userIdC = UserId(3)

  val userA = User(userIdA, now, now, "user.a@test.tst", "A First", "A Last", "a-password", true, Some(SsoUserId("sso-a")))
  val userB = User(userIdB, now, now, "user.b@test.tst", "B First", "B Last", "b-password", true, Some(SsoUserId("sso-b")))
  val userC = User(userIdC, now, now, "user.c@test.tst", "C First", "C Last", "c-password", true, None)

  val privilegeC1 = Privilege(PrivilegeId(1), now, userIdC, RoleId(UserRole.CustomerServicesRep.id))
  val privilegeC2 = Privilege(PrivilegeId(2), now, userIdC, RoleId(UserRole.Employee.id))

  def fullUserPatch = UserPatch(Some("Updated First"), Some("Updated Last"), Some("updated@test.tst"), Some(false), None)

  val authenticatedUserA = AuthenticatedUser(userIdA.value, None, "bbb-access-token", Map[String, AnyRef]("sso/at" -> "some-access-token"))
  val authenticatedUserB = AuthenticatedUser(userIdB.value, None, "bbb-access-token", Map.empty)
  val authenticatedUserC = AuthenticatedUser(userIdC.value, None, "bbb-access-token", Map.empty)

  val clientIdA1 = ClientId(1)
  val clientIdA2 = ClientId(2)
  val clientIdA3 = ClientId(3)

  val clientA1 = Client(clientIdA1, now, now, userIdA, "Client A1", "Test brand A1", "Test model A1", "Test OS A1", "test-secret-a1", false)
  val clientA2 = Client(clientIdA2, now, now, userIdA, "Client A2", "Test brand A2", "Test model A2", "Test OS A2", "test-secret-a2", false)
  val clientA3 = Client(clientIdA3, now, now, userIdA, "Client A3", "Test brand A3", "Test model A3", "Test OS A3", "test-secret-a3", true)

  val exp = now.withDurationAdded(Duration.standardHours(1), 1)
  val refreshTokenClientA1Id = RefreshTokenId(1)
  val refreshTokenClientA1 = RefreshToken(
    refreshTokenClientA1Id, now, now, userIdA, Some(clientIdA1), "some-token-a1", Some(SsoRefreshToken("some-sso-token-a1")), false, exp, exp, exp)
  val refreshTokenClientA2 = RefreshToken(
    RefreshTokenId(2), now, now, userIdA, Some(clientIdA2), "some-token-a2", Some(SsoRefreshToken("some-sso-token-a2")), false, exp, exp, exp)
  val refreshTokenClientA3 = RefreshToken(
    RefreshTokenId(3), now, now, userIdA, Some(clientIdA3), "some-token-a3", Some(SsoRefreshToken("some-sso-token-a3")), true, now, now, now)
  val refreshTokenNoClientA = RefreshToken(
    RefreshTokenId(4), now, now, userIdA, None, "some-token-a", Some(SsoRefreshToken("some-sso-token-a")), false, now, now, now)
  val refreshTokenNoClientDeregisteredA = RefreshToken(
    RefreshTokenId(5), now, now, userIdA, None, "some-token-a-deregistered", Some(SsoRefreshToken("some-sso-token-a-deregistered")), true, now, now, now)

  val refreshTokenNoClientC = RefreshToken(
    RefreshTokenId(6), now, now, userIdC, None, "some-token-c", Some(SsoRefreshToken("some-sso-token")), false, now, now, now)

  val clientsC =
    for (id <- 4 until 16)
    yield Client(ClientId(id), now, now, userIdC, s"Client C$id", s"Test brand C$id", s"Test model C$id", s"Test OS C$id", s"test-secret-c$id", false)

  val clientInfoA1 = ClientInfo(clientIdA1.external, "/clients/1", "Client A1", "Test brand A1", "Test model A1", "Test OS A1", None, now.toLocalDate)
  val clientInfoA2 = ClientInfo(clientIdA2.external, "/clients/2", "Client A2", "Test brand A2", "Test model A2", "Test OS A2", None, now.toLocalDate)

  val fullClientPatch = ClientPatch(Some("Patched name"), Some("Patched brand"), Some("Patched model"), Some("Patched OS"))
  val fullPatchedClientA1 = clientA1.copy(name = "Patched name", brand = "Patched brand", model = "Patched model", os = "Patched OS")
  val fullPatchedClientInfoA1 = clientInfoA1.copy(client_name = "Patched name", client_brand = "Patched brand", client_model = "Patched model", client_os = "Patched OS")

  val forbiddenClientAccesses = (clientIdA3, authenticatedUserA) ::
    (clientIdA1, authenticatedUserB) ::
    (clientIdA2, authenticatedUserB) ::
    (clientIdA3, authenticatedUserB) ::
    (ClientId(100), authenticatedUserA) ::
    (ClientId(100), authenticatedUserB) ::Nil

  val clientRegistration = ClientRegistration("Test name", "Test brand", "Test model", "Test OS")

  val resetCredentials = ResetTokenCredentials(SsoPasswordResetToken("res3tt0ken"), "new-password", Some(clientIdA1.external), Some("test-secret-a1"))

  val userBPreviousUsername1 = PreviousUsername(PreviousUsernameId(1), now.minusDays(4), userIdB, "previous.userb.1@test.tst")
  val userBPreviousUsername2 = PreviousUsername(PreviousUsernameId(2), now.minusDays(2), userIdB, "previous.userb.2@test.tst")

  val adminInfoUserA = AdminUserInfo(userIdA.external, userIdA.uri, userA.username, userA.firstName, userA.lastName, userA.allowMarketing, Nil)
  val adminInfoUserB = AdminUserInfo(
    userIdB.external, userIdB.uri, userB.username, userB.firstName, userB.lastName, userB.allowMarketing,
    PreviousUsernameInfo(userBPreviousUsername2.username, userBPreviousUsername2.createdAt) ::
    PreviousUsernameInfo(userBPreviousUsername1.username, userBPreviousUsername1.createdAt) :: Nil)

  def removeSSOTokens(): Unit = {
    import driver.simple._
    db.withSession { implicit session =>
      tables.refreshTokens.map(_.ssoToken).update(None)
    }
  }

  def preSyncUser(id: UserId): Unit = db.withSession { implicit session =>
    import tables._
    users.filter(_.id === id).map(_.ssoId).update(Some(SsoUserId("B0E8428E-7DEB-40BF-BFBE-5D0927A54F65")))
  }

  def setUsername(id: UserId, username: String): Unit = db.withSession { implicit session =>
    import tables._
    users.filter(_.id === id).map(_.username).update(username)
  }

  def ssoRequests: List[HttpRequest] = ssoClient.requests

  def cleanup(): Unit = {
    import tables.driver.simple._

    db.withSession { implicit session =>
      val ddl = tables.users.ddl ++ tables.clients.ddl ++ tables.refreshTokens.ddl ++ tables.loginAttempts.ddl ++
        tables.roles.ddl ++ tables.privileges.ddl ++ tables.previousUsernames.ddl

      try {
        ddl.drop
      } catch { case _: JdbcSQLException => /* Do nothing */ }

      ddl.create
    }

    db.withSession { implicit session =>
      tables.users ++= Seq(userA, userB, userC)
      tables.clients ++= Seq(clientA1, clientA2, clientA3) ++ clientsC
      tables.refreshTokens ++= Seq(refreshTokenClientA1, refreshTokenClientA2, refreshTokenClientA3, refreshTokenNoClientA, refreshTokenNoClientDeregisteredA)
      tables.roles.forceInsertAll(UserRole.values.map(r => Role(RoleId(r.id), r, r.toString + " description")).toSeq: _*)
      tables.privileges ++= Seq(privilegeC1, privilegeC2)
      tables.previousUsernames ++= Seq(userBPreviousUsername1, userBPreviousUsername2)
    }

    publisher.events = Nil
    ssoResponse.reset()
    ssoClient.reset()
  }

  cleanup()

  val tokenInfoA1 = tokenBuilder.issueAccessToken(
    userA, None, refreshTokenNoClientA, Some(SsoCredentials(SsoAccessToken("some-access-token"), "bearer", 300, SsoRefreshToken("some-refresh-token"))))

  val tokenInfoC = tokenBuilder.issueAccessToken(
    userC, None, refreshTokenNoClientC, Some(SsoCredentials(SsoAccessToken("some-access-token"), "bearer", 300, SsoRefreshToken("some-refresh-token"))))

  val tokenInfoA1WithoutSSO = tokenBuilder.issueAccessToken(userA, None, refreshTokenNoClientA, None)
}
