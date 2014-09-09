package com.blinkbox.books.auth.server.cake

import java.util.concurrent.ForkJoinPool

import akka.actor.ActorSystem
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{LegacyRabbitMqPublisher, Publisher, RabbitMqPublisher}
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso._
import com.blinkbox.books.auth.{User, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.rabbitmq.RabbitMq
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray._
import com.blinkbox.books.time._
import com.rabbitmq.client.Connection
import com.zaxxer.hikari.HikariDataSource
import spray.http.Uri.Path
import spray.routing.RouteConcatenation._

import scala.concurrent.ExecutionContext
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

trait DefaultConfigComponent extends ConfigComponent {
  override val config = AppConfig.default
}

trait DefaultAsyncComponent extends AsyncComponent {
  override val actorSystem: ActorSystem = ActorSystem("auth-server")
  override val apiExecutionContext = DiagnosticExecutionContext(actorSystem.dispatcher)
  override val ssoClientExecutionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutorService(new ForkJoinPool))
  override val serviceExecutionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutorService(new ForkJoinPool))
  override val rabbitExecutionContext = DiagnosticExecutionContext(ExecutionContext.fromExecutorService(new ForkJoinPool))
}

trait DefaultEventsComponent extends EventsComponent {
  this: ConfigComponent with AsyncComponent with TimeSupport =>

  private val rabbitConnection: Connection = RabbitMq.reliableConnection(config.rabbit)
  override val publisher: Publisher = withRabbitContext { implicit ec =>
    new RabbitMqPublisher(rabbitConnection.createChannel) ~ new LegacyRabbitMqPublisher(rabbitConnection.createChannel)
  }
}

trait DefaultDatabaseComponent extends DatabaseComponent {
  this: ConfigComponent =>

  override val DB = new MySQLDatabaseSupport

  override val driver = MySQLDriver

  override val db = {
    val c = config.db

    val poolConfig = config.hikari.get
    poolConfig.addDataSourceProperty("serverName", c.host)
    poolConfig.addDataSourceProperty("port", c.port.getOrElse(3306))
    poolConfig.addDataSourceProperty("databaseName", c.db)
    poolConfig.setUsername(c.user)
    poolConfig.setPassword(c.pass)

    Database.forDataSource(new HikariDataSource(poolConfig))
  }

  override val tables = ZuulTables[DB.Profile](driver)
}

trait DefaultPasswordHasherComponent extends PasswordHasherComponent {
  override val passwordHasher = PasswordHasher.default
}

trait DefaultTokenBuilderComponent extends TokenBuilderComponent {
  this: ConfigComponent =>

  override val tokenBuilder = new DefaultTokenBuilder(config.authServer.keysConfig)
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
    with SsoComponent =>

  val sessionService = withServiceContext { implicit ec =>
    new DefaultSessionService(db, authRepository, userRepository, clientRepository, geoIp, publisher, sso)
  }
}

trait DefaultRegistrationServiceComponent extends RegistrationServiceComponent {
  this: ConfigComponent
    with DatabaseComponent
    with RepositoriesComponent
    with GeoIPComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with TokenBuilderComponent
    with SsoComponent =>

  val registrationService = withServiceContext { implicit ec =>
    new DefaultRegistrationService(
      db, authRepository, userRepository, clientRepository, exceptionFilter, config.authServer.termsVersion, tokenBuilder, geoIp, publisher, sso)
  }
}

trait DefaultUserServiceComponent extends UserServiceComponent {
  this: DatabaseComponent
    with RepositoriesComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with SsoComponent =>

  val userService = withServiceContext { implicit ec =>
    new DefaultUserService(db, userRepository, sso, publisher)
  }
}

trait DefaultClientServiceComponent extends ClientServiceComponent {
  this: ConfigComponent
    with DatabaseComponent
    with RepositoriesComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport =>

  val clientService = withServiceContext { implicit ec =>
    new DefaultClientService(
      db, clientRepository, authRepository, userRepository, config.authServer.maxClients, publisher)
  }
}

trait DefaultPasswordAuthenticationServiceComponent extends PasswordAuthenticationServiceComponent {
  this: DatabaseComponent
    with RepositoriesComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with SsoComponent
    with TokenBuilderComponent
    with SsoSyncComponent =>

  val passwordAuthenticationService = withServiceContext { implicit ec =>
    new DefaultPasswordAuthenticationService(
      db, authRepository, userRepository, tokenBuilder, publisher, ssoSync, sso)
  }
}

trait DefaultRefreshTokenServiceComponent extends RefreshTokenServiceComponent {
  this: ConfigComponent
    with DatabaseComponent
    with RepositoriesComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with TokenBuilderComponent
    with SsoComponent =>

  val refreshTokenService = withServiceContext { implicit ec =>
    new DefaultRefreshTokenService(
      db, authRepository, userRepository, clientRepository, config.authServer.refreshTokenLifetimeExtension, tokenBuilder, publisher, sso)
  }
}

trait DefaultSsoSyncComponent extends SsoSyncComponent {
  this: ConfigComponent
    with EventsComponent
    with AsyncComponent
    with SsoComponent
    with DatabaseComponent
    with RepositoriesComponent =>

  def ssoSync = withServiceContext { implicit ec =>
    new DefaultSsoSyncService(db, userRepository, config.authServer.termsVersion, publisher, sso)
  }
}

trait DefaultPasswordUpdatedServiceComponent extends PasswordUpdateServiceComponent {
  this: ConfigComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with SsoComponent
    with DatabaseComponent
    with RepositoriesComponent
    with TokenBuilderComponent
    with SsoSyncComponent =>

  val passwordUpdateService = withServiceContext { implicit ec =>
    new DefaultPasswordUpdateService(
      db, userRepository, authRepository, config.authServer.passwordResetBaseUrl, tokenBuilder, ssoSync, publisher, sso)
  }
}

trait DefaultSsoComponent extends SsoComponent {
  this: ConfigComponent with AsyncComponent =>

  private val keyStore = new FileKeyStore(config.sso.keyStore)
  private val tokenDecoder = new SsoAccessTokenDecoder(keyStore)
  override val sso = withSsoClientContext { implicit ec =>
    new DefaultSso(config.sso, new DefaultClient(config.sso), tokenDecoder)(ssoClientExecutionContext)
  }
}

trait DefaultApiComponent extends ApiComponent {
  this: AuthServiceComponent
    with ClientServiceComponent
    with UserServiceComponent
    with RegistrationServiceComponent
    with PasswordAuthenticationServiceComponent
    with RefreshTokenServiceComponent
    with PasswordUpdateServiceComponent
    with SsoComponent
    with ConfigComponent
    with AsyncComponent =>

  val authenticator: ElevatedContextAuthenticator[User] = withApiContext { implicit ec =>
    new BearerTokenAuthenticator(
      new ZuulTokenDeserializer(new ZuulTokenDecoder(config.authClient.keysDir.getAbsolutePath)),
      new SsoElevationChecker(sso))
  }

  private val zuulApi = new AuthApi(config.service,
    userService,
    clientService,
    sessionService,
    registrationService,
    passwordAuthenticationService,
    refreshTokenService,
    passwordUpdateService,
    authenticator)

  private val healthApi = new HealthCheckHttpService {
    override val basePath = Path./
    override implicit def actorRefFactory = actorSystem
  }

  override val routes = healthApi.routes ~ zuulApi.routes
}
