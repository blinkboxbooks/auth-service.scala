package com.blinkbox.books.auth.server

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConfig
import com.typesafe.config.Config
import com.blinkbox.books.config.Configuration

case class AppConfig(service: ApiConfig, swagger: SwaggerConfig, db: DatabaseConfig, rabbit: RabbitMqConfig, auth: AuthClientConfig)

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.auth.api.public"),
    SwaggerConfig(config, 1),
    DatabaseConfig(config, "service.auth.db"),
    RabbitMqConfig(config),
    AuthClientConfig(config))

  def default: AppConfig = AppConfig((new Configuration {}).config)
}
