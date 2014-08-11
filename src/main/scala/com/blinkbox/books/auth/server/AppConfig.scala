package com.blinkbox.books.auth.server

import java.util.concurrent.TimeUnit

import com.blinkbox.books.config.{Configuration, _}
import com.blinkbox.books.rabbitmq.RabbitMqConfig
import com.typesafe.config.Config
import java.nio.file.{Files, Paths}
import spray.http.{BasicHttpCredentials, HttpCredentials}

import scala.concurrent.duration._

case class AppConfig(service: ApiConfig, swagger: SwaggerConfig, db: DatabaseConfig, rabbit: RabbitMqConfig, auth: AuthClientConfig, sso: SSOConfig)
case class SSOConfig(host: String, port: Int, version: String, credentials: HttpCredentials, timeout: FiniteDuration, keyStore: String) {
  require(Files.exists(Paths.get(keyStore)), s"SSO key-store file not found: $keyStore")
}

object SSOConfig {
  def apply(config: Config): SSOConfig = SSOConfig(
    host = config.getString("sso.host"),
    port = config.getInt("sso.port"),
    version = config.getString("sso.version"),
    credentials = BasicHttpCredentials(config.getString("sso.credentials.username"), config.getString("sso.credentials.password")),
    timeout = config.getDuration("sso.timeout", TimeUnit.MILLISECONDS).millis,
    keyStore = config.getString("sso.keyStore")
  )
}

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.auth.api.public"),
    SwaggerConfig(config, 1),
    DatabaseConfig(config, "service.auth.db"),
    RabbitMqConfig(config),
    AuthClientConfig(config),
    SSOConfig(config))

  def default: AppConfig = AppConfig((new Configuration {}).config)
}
