package com.blinkbox.books.auth.server

import akka.actor.Props
import akka.io.IO
import com.blinkbox.books.auth.server.cake._
import com.blinkbox.books.auth.server.services.GeoIP
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.spray._
import com.blinkbox.books.time.SystemTimeSupport
import spray.can.Http
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.http.{AllOrigins, RemoteAddress}
import spray.routing._

// TODO: Real GeoIP checking
object DummyGeoIP extends GeoIP {
  override def countryCode(address: RemoteAddress): String = "GB"
}

object WebAppComponents extends
  DefaultConfigComponent with
  DefaultAsyncComponent with
  SystemTimeSupport with
  DefaultSSOComponent with
  DefaultGeoIPComponent with
  DefaultEventsComponent with
  DefaultDatabaseComponent with
  DefaultPasswordHasherComponent with
  DefaultRepositoriesComponent with
  DefaultUserServiceComponent with
  DefaultClientServiceComponent with
  DefaultAuthServiceComponent with
  DefaultRegistrationServiceComponent with
  DefaultApiComponent

class WebService extends HttpServiceActor {
  val route = WebAppComponents.zuulRoutes ~ respondWithHeader(`Access-Control-Allow-Origin`(AllOrigins)) { WebAppComponents.swaggerRoutes }
  def receive = runRoute(route)
}

object WebApp extends App with Configuration with Loggers {
  implicit val system = WebAppComponents.system

  sys.addShutdownHook(system.shutdown())

  val service = system.actorOf(Props[WebService], "web-service")
  val localUrl = WebAppComponents.config.service.localUrl

  IO(Http) ! Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort)
}
