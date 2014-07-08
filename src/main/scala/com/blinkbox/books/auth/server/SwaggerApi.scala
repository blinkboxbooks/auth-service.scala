package com.blinkbox.books.auth.server

import akka.actor.ActorRefFactory
import com.blinkbox.books.config.SwaggerConfig
import com.gettyimages.spray.swagger.SwaggerHttpService
import scala.reflect.runtime.universe._

class SwaggerApi(config: SwaggerConfig)(implicit val actorRefFactory: ActorRefFactory) extends {
  override val apiTypes = Seq(typeOf[UserRoutes])
  override val apiVersion = "1.0"
  override val baseUrl = config.baseUrl.toString
  override val docsPath = config.docsPath
} with SwaggerHttpService 
