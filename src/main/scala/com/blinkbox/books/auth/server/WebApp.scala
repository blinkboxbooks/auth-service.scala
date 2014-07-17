package com.blinkbox.books.auth.server

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.data.MySqlAuthRepository
import com.blinkbox.books.auth.{Elevation, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.spray._
import com.blinkbox.books.time.SystemTimeSupport
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http.{RemoteAddress, AllOrigins}
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.routing._

import scala.concurrent.{ExecutionContext, Future}

class DummyNotifier(implicit ec: ExecutionContext) extends Notifier {
  val log = LoggerFactory.getLogger(classOf[Notifier])
  override def notifyUserCreated(user: User, client: Option[Client]) = Future { log.info(s"User created: $user, $client") }
  override def notifyUserAuthenticated(user: User, client: Option[Client]) = Future { log.info(s"User authenticated: $user, $client") }
}

object DummyGeoIP extends GeoIP {
  override def countryCode(address: RemoteAddress): String = "GB"
}

class WebService(config: AppConfig) extends HttpServiceActor with SystemTimeSupport {
  implicit val executionContext = actorRefFactory.dispatcher
  val authenticator = new ZuulTokenAuthenticator(
    new ZuulTokenDeserializer(new ZuulTokenDecoder(config.auth.keysDir.getAbsolutePath)),
    _ => Future.successful(Elevation.Critical)) // TODO: Use a real in-proc elevation checker!
  val service = new DefaultAuthService(config.db, new MySqlAuthRepository(config.db), DummyGeoIP, new DummyNotifier)
  val users = new AuthApi(config.service, service, authenticator)
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
