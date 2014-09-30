package com.blinkbox.books.auth.server

import akka.actor.Props
import akka.util.Timeout
import com.blinkbox.books.auth.server.cake._
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.spray._
import com.blinkbox.books.time.SystemTimeSupport
import com.typesafe.scalalogging.slf4j.StrictLogging
import spray.can.Http
import spray.routing._

import scala.concurrent.duration._

object WebAppComponents extends
  DefaultConfigComponent with
  DefaultAsyncComponent with
  SystemTimeSupport with
  DefaultSsoComponent with
  DefaultGeoIPComponent with
  DefaultEventsComponent with
  DefaultDatabaseComponent with
  DefaultTokenBuilderComponent with
  DefaultRepositoriesComponent with
  DefaultServicesComponent with
  DefaultApiComponent

class WebService extends HttpServiceActor {
  def receive = runRoute(WebAppComponents.routes)
}

object WebApp extends App with Configuration with Loggers with StrictLogging {
  try {
    implicit val system = WebAppComponents.actorSystem
    implicit val executionContext = system.dispatcher
    implicit val startTimeout = Timeout(10.seconds)

    sys.addShutdownHook(system.shutdown())

    val service = system.actorOf(Props[WebService], "web-service")
    val localUrl = WebAppComponents.config.service.localUrl

    HttpServer(Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort))
  } catch {
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      System.exit(1)
  }
}
