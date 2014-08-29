package com.blinkbox.books.auth.server.cake

import akka.actor.ActorSystem
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{LegacyRabbitMqPublisher, Publisher, RabbitMqPublisher}
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.{DefaultClient, DefaultSSO, FileKeyStore, SsoAccessTokenDecoder}
import com.blinkbox.books.auth.server.{AppConfig, AuthApi, DummyGeoIP, PasswordHasher, SwaggerApi}
import com.blinkbox.books.auth.{Elevation, User, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.rabbitmq.RabbitMq
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray._
import com.blinkbox.books.time._
import com.rabbitmq.client.Connection
import spray.routing.Route
import spray.routing.authentication.ContextAuthenticator

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

trait DefaultConfigComponent extends ConfigComponent {
  override val config = AppConfig.default
}

trait DefaultAsyncComponent extends AsyncComponent {
  override val actorSystem: ActorSystem = ActorSystem("auth-server")
  override val executionContext: ExecutionContext = actorSystem.dispatcher
}

trait DefaultEventsComponent extends EventsComponent {
  this: ConfigComponent with AsyncComponent with TimeSupport =>
  private val rabbitConnection: Connection = RabbitMq.reliableConnection(config.rabbit)
  override val publisher: Publisher = new RabbitMqPublisher(rabbitConnection.createChannel) ~ new LegacyRabbitMqPublisher(rabbitConnection.createChannel)
}

trait DefaultDatabaseComponent extends DatabaseComponent {
  this: ConfigComponent =>

  override val DB = new MySQLDatabaseSupport

  override val driver = MySQLDriver

  override val db = {
    val jdbcUrl = s"jdbc:${config.db.uri.withUserInfo("")}"
    val Array(user, password) = config.db.uri.getUserInfo.split(':')
    Database.forURL(jdbcUrl, driver = "com.mysql.jdbc.Driver", user = user, password = password)
  }

  override val tables = ZuulTables[DB.Profile](driver)
}

trait DefaultPasswordHasherComponent extends PasswordHasherComponent {
  override val passwordHasher = PasswordHasher.default
}

trait DefaultRepositoriesComponent extends RepositoriesComponent {
  this: DatabaseComponent with PasswordHasherComponent with TimeSupport =>

  override val authRepository = new DefaultAuthRepository(tables)
  override val userRepository = new DefaultUserRepository(tables, passwordHasher)
  override val clientRepository = new DefaultClientRepository(tables)
}

trait DefaultGeoIPComponent extends GeoIPComponent {
  def geoIp = DummyGeoIP
}

trait DefaultAuthServiceComponent extends AuthServiceComponent {
  this: DatabaseComponent
    with RepositoriesComponent
    with GeoIPComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with SSOComponent =>

  val sessionService = new DefaultSessionService(db, authRepository, userRepository, clientRepository, geoIp, publisher, sso)
}

trait DefaultRegistrationServiceComponent extends RegistrationServiceComponent {
  this: DatabaseComponent
    with RepositoriesComponent
    with GeoIPComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with SSOComponent =>

  val registrationService = new DefaultRegistrationService(db, authRepository, userRepository, clientRepository, exceptionFilter, geoIp, publisher, sso)
}

trait DefaultUserServiceComponent extends UserServiceComponent {
  this: DatabaseComponent with RepositoriesComponent with EventsComponent with AsyncComponent with TimeSupport with SSOComponent =>

  val userService = new DefaultUserService(db, userRepository, sso, publisher)
}

trait DefaultClientServiceComponent extends ClientServiceComponent {
  this: DatabaseComponent with RepositoriesComponent with EventsComponent with AsyncComponent with TimeSupport =>

  val clientService = new DefaultClientService(db, clientRepository, authRepository, userRepository, publisher)
}

trait DefaultPasswordAuthenticationServiceComponent extends PasswordAuthenticationServiceComponent {
  this: DatabaseComponent with RepositoriesComponent with EventsComponent with AsyncComponent with TimeSupport with SSOComponent =>

  val passwordAuthenticationService = new DefaultPasswordAuthenticationService(db, authRepository, userRepository, clientRepository, publisher, sso)
}

trait DefaultRefreshTokenServiceComponent extends RefreshTokenServiceComponent {
  this: DatabaseComponent with RepositoriesComponent with EventsComponent with AsyncComponent with TimeSupport with SSOComponent =>

  val refreshTokenService = new DefaultRefreshTokenService(db, authRepository, userRepository, clientRepository, publisher, sso)
}

trait DefaultPasswordUpdatedServiceComponent extends PasswordUpdateServiceComponent {
  this: EventsComponent with AsyncComponent with TimeSupport with SSOComponent =>

  val passwordUpdateService = new DefaultPasswordUpdateService(publisher, sso)
}

trait DefaultSSOComponent extends SSOComponent {
  this: ConfigComponent with AsyncComponent =>

  private val keyStore = new FileKeyStore(config.sso.keyStore)
  private val tokenDecoder = new SsoAccessTokenDecoder(keyStore)
  override val sso = new DefaultSSO(config.sso, new DefaultClient(config.sso), tokenDecoder)
}

trait DefaultApiComponent {
  this: AuthServiceComponent
    with ClientServiceComponent
    with UserServiceComponent
    with RegistrationServiceComponent
    with PasswordAuthenticationServiceComponent
    with RefreshTokenServiceComponent
    with PasswordUpdateServiceComponent
    with ConfigComponent
    with AsyncComponent =>

  val authenticator: ContextAuthenticator[User] = new ZuulTokenAuthenticator(
    new ZuulTokenDeserializer(new ZuulTokenDecoder(config.auth.keysDir.getAbsolutePath)),
    _ => Future.successful(Elevation.Critical)) // TODO: Use a real in-proc elevation checker

  private val zuulApi = new AuthApi(config.service,
    userService,
    clientService,
    sessionService,
    registrationService,
    passwordAuthenticationService,
    refreshTokenService,
    passwordUpdateService,
    authenticator)
  private val swaggerApi = new SwaggerApi(config.swagger)

  val zuulRoutes: Route = zuulApi.routes
  val swaggerRoutes: Route = swaggerApi.routes
}
