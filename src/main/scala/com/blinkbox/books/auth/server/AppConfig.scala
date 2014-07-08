package com.blinkbox.books.auth.server

import com.blinkbox.books.config._
import com.typesafe.config.Config

case class AppConfig(service: ApiConfig, swagger: SwaggerConfig, db: DatabaseConfig)

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.auth.api.public"),
    SwaggerConfig(config, 1),
    DatabaseConfig(config, "service.auth.db"))
}
