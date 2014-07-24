package com.blinkbox.books.auth.server

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.blinkbox.books.auth.server.data.{ZuulTables, DefaultUserRepository, DefaultAuthRepository}
import com.blinkbox.books.auth.server.events.{LegacyRabbitMqPublisher, RabbitMqPublisher}
import com.blinkbox.books.auth.{Elevation, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.spray._
import com.blinkbox.books.time.SystemTimeSupport
import spray.can.Http
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.http.{AllOrigins, RemoteAddress}
import spray.routing._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

// TODO: Real GeoIP checking
object DummyGeoIP extends GeoIP {
  override def countryCode(address: RemoteAddress): String = "GB"
}

class WebService(config: AppConfig) extends HttpServiceActor with SystemTimeSupport {
  implicit val executionContext = actorRefFactory.dispatcher

  val authenticator = new ZuulTokenAuthenticator(
    new ZuulTokenDeserializer(new ZuulTokenDecoder(config.auth.keysDir.getAbsolutePath)),
    _ => Future.successful(Elevation.Critical)) // TODO: Use a real in-proc elevation checker!

  val notifier = new RabbitMqPublisher ~ new LegacyRabbitMqPublisher(config.rabbit)

  val db = {
    val jdbcUrl = s"jdbc:${config.db.uri.withUserInfo("")}"
    val Array(user, password) = config.db.uri.getUserInfo.split(':')
    Database.forURL(jdbcUrl, driver = "com.mysql.jdbc.Driver", user = user, password = password)
  }

  val dbDriver = MySQLDriver

  val geoIp = DummyGeoIP

  val passwordHasher = PasswordHasher.default

  val tables = ZuulTables(dbDriver)

  val authRepository = new DefaultAuthRepository(tables)
  val userRepository = new DefaultUserRepository(tables, passwordHasher)

  val authService = new DefaultAuthService(db, authRepository, userRepository, geoIp, notifier)
  val userService = new DefaultUserService(db, userRepository, geoIp, notifier)

  val users = new AuthApi(config.service, userService, authService, authenticator)
  val swagger = new SwaggerApi(config.swagger)

  val route = users.routes ~ respondWithHeader(`Access-Control-Allow-Origin`(AllOrigins)) { swagger.routes }

  def receive = runRoute(route)
}

object WebApp extends App with Configuration with Loggers {
  implicit val system = ActorSystem("auth-server")
  sys.addShutdownHook(system.shutdown())
  val appConfig = AppConfig(config)
  val service = system.actorOf(Props(classOf[WebService], appConfig), "web-service")
  val localUrl = appConfig.service.localUrl
  IO(Http) ! Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort)
}
