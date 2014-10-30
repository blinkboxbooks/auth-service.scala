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
  this: ConfigComponent =>
  override val actorSystem: ActorSystem = ActorSystem("auth-server", config.raw)
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

trait DefaultTokenBuilderComponent extends TokenBuilderComponent {
  this: ConfigComponent with DatabaseComponent with RepositoriesComponent =>

  override lazy val tokenBuilder = new DefaultTokenBuilder(config.authServer.keysConfig, db, roleRepository)
}

trait DefaultRepositoriesComponent extends RepositoriesComponent {
  this: DatabaseComponent with TimeSupport =>

  override val authRepository = new DefaultAuthRepository(tables)
  override val userRepository = new DefaultUserRepository(tables)
  override val clientRepository = new DefaultClientRepository(tables)
  override val roleRepository = new DefaultRoleRepository(tables)
}

trait DefaultGeoIPComponent extends GeoIPComponent {
  override val geoIp = new MaxMindGeoIP
}

trait DefaultServicesComponent extends ServicesComponent {
  this: ConfigComponent
    with DatabaseComponent
    with RepositoriesComponent
    with GeoIPComponent
    with EventsComponent
    with AsyncComponent
    with TimeSupport
    with TokenBuilderComponent
    with SsoComponent =>

  private implicit val ec = serviceExecutionContext

  lazy val sessionService = new DefaultSessionService(db, authRepository, userRepository, clientRepository, geoIp, publisher, sso)

  lazy val registrationService = new DefaultRegistrationService(
      db, authRepository, userRepository, clientRepository, exceptionFilter, config.authServer.termsVersion, tokenBuilder, geoIp, publisher, sso)

  lazy val userService = new DefaultUserService(db, userRepository, ssoSync, sso, publisher)

  lazy val clientService = new DefaultClientService(db, clientRepository, authRepository, userRepository, config.authServer.maxClients, sso, publisher)

  lazy val passwordAuthenticationService = new DefaultPasswordAuthenticationService(db, authRepository, userRepository, tokenBuilder, publisher, ssoSync, sso)

  lazy val refreshTokenService = new DefaultRefreshTokenService(db, authRepository, userRepository, clientRepository,
    config.authServer.refreshTokenLifetimeExtension, tokenBuilder, publisher, sso)

  lazy val ssoSync = new DefaultSsoSyncService(db, userRepository, config.authServer.termsVersion, publisher, sso)

  lazy val passwordUpdateService = new DefaultPasswordUpdateService(db, userRepository, authRepository,
    config.authServer.passwordResetBaseUrl, tokenBuilder, ssoSync, publisher, sso)

  lazy val adminUserService =  new DefaultAdminUserService(db, userRepository)
}

trait DefaultSsoComponent extends SsoComponent {
  this: ConfigComponent with AsyncComponent =>

  private val keyStore = new FileKeyStore(config.sso.keyPath)
  private val tokenDecoder = new SsoAccessTokenDecoder(keyStore)
  override val sso = withSsoClientContext { implicit ec =>
    new DefaultSso(config.sso, new DefaultClient(config.sso), tokenDecoder)(ssoClientExecutionContext)
  }
}

trait DefaultApiComponent extends ApiComponent {
  this: ServicesComponent
    with SsoComponent
    with ConfigComponent
    with AsyncComponent =>

  val authenticator: ElevatedContextAuthenticator[User] = withApiContext { implicit ec =>
    new BearerTokenAuthenticator(
      new ZuulTokenDeserializer(new ZuulTokenDecoder(config.authServer.keysConfig.path.toString)),
      withSsoClientContext(ssoContext => new SsoElevationChecker(sso)(ssoContext)))
  }

  private val zuulApi = new AuthApi(config.service,
    userService,
    clientService,
    sessionService,
    registrationService,
    passwordAuthenticationService,
    refreshTokenService,
    passwordUpdateService,
    adminUserService,
    authenticator)

  private val healthApi = new HealthCheckHttpService {
    override val basePath = Path./
    override implicit def actorRefFactory = actorSystem
  }

  override val routes = healthApi.routes ~ zuulApi.routes
}
