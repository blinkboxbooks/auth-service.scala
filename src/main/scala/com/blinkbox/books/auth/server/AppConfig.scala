package com.blinkbox.books.auth.server

import java.nio.file.{Path, Files, Paths}
import java.util.concurrent.TimeUnit

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConfig
import com.typesafe.config.Config
import spray.http.{BasicHttpCredentials, HttpCredentials}

import scala.concurrent.duration._

case class AppConfig(
    service: ApiConfig,
    swagger: SwaggerConfig,
    db: DatabaseConfig,
    rabbit: RabbitMqConfig,
    authClient: AuthClientConfig,
    sso: SsoConfig,
    authServer: AuthServerConfig)

case class KeysConfig(path: String, signingKeyId: String, encryptionKeyId: String) {
  val signingKeyPath: Path = Paths.get(path, signingKeyId, "private.key")
  val encryptionKeyPath: Path = Paths.get(path, encryptionKeyId, "public.key")

  require(Files.exists(signingKeyPath), s"Auth service signing key not found: $signingKeyPath")
  require(Files.exists(encryptionKeyPath), s"Auth service signing key not found: $encryptionKeyPath")
}

case class AuthServerConfig(
    maxClients: Int,
    termsVersion: String,
    passwordResetBaseUrl: String,
    keysConfig: KeysConfig,
    refreshTokenLifetimeExtension: FiniteDuration) {
}

case class SsoConfig(
    host: String,
    port: Int,
    version: String,
    credentials: HttpCredentials,
    timeout: FiniteDuration,
    keyStore: String) {

  require(Files.exists(Paths.get(keyStore)), s"SSO key-store file not found: $keyStore")
}

object KeysConfig {
  def apply(config: Config, prefix: String): KeysConfig = KeysConfig(
    path = config.getString(s"$prefix.path"),
    signingKeyId = config.getString(s"$prefix.signing"),
    encryptionKeyId = config.getString(s"$prefix.encryption")
  )
}

object AuthServerConfig {
  def apply(config: Config, prefix: String): AuthServerConfig = AuthServerConfig(
    maxClients = config.getInt(s"$prefix.maxClients"),
    termsVersion = config.getString(s"$prefix.termsVersion"),
    passwordResetBaseUrl = config.getString(s"$prefix.passwordResetBaseUrl"),
    keysConfig = KeysConfig(config, s"$prefix.keys"),
    refreshTokenLifetimeExtension = config.getDuration(s"$prefix.refreshTokenLifetimeExtension", TimeUnit.MILLISECONDS).millis
  )
}

object SsoConfig {
  def apply(config: Config): SsoConfig = SsoConfig(
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
    SsoConfig(config),
    AuthServerConfig(config, "service.auth"))

  lazy val default: AppConfig = AppConfig((new Configuration {}).config)
}
