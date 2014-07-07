package com.blinkbox.books.agora

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.blinkbox.books.auth.{ZuulElevationChecker, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.config.Configuration
import com.blinkbox.books.spray._
import spray.can.Http
import spray.http.{AllOrigins, Uri}
import spray.http.HttpHeaders.`Access-Control-Allow-Origin`
import spray.routing._

class WebService(config: AppConfig) extends HttpServiceActor {
  implicit val executionContext = actorRefFactory.dispatcher
//  val authenticator = new ZuulTokenAuthenticator(
//    new ZuulTokenDeserializer(new ZuulTokenDecoder(config.auth.keysDir.getAbsolutePath)),
//    new ZuulElevationChecker(config.auth.sessionUrl.toString))
  val service = new DefaultUserService(config.db)
  val users = new UserApi(config.service, service)
  val swagger = new SwaggerApi(config.swagger)
  val route = users.routes ~ respondWithHeader(`Access-Control-Allow-Origin`(AllOrigins)) { swagger.routes }
  def receive = runRoute(route)
}

object WebApp extends App with Configuration {
  implicit val system = ActorSystem("auth-server")
  sys.addShutdownHook(system.shutdown())
  val appConfig = AppConfig(config)
  val service = system.actorOf(Props(classOf[WebService], appConfig), "web-service")
  val localUrl = appConfig.service.localUrl
  IO(Http) ! Http.Bind(service, localUrl.getHost, port = localUrl.effectivePort)
}
