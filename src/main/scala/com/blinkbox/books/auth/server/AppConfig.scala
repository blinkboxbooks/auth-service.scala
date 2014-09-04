package com.blinkbox.books.auth.server

import java.nio.file.{Files, Path, Paths}
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.blinkbox.books.config._
import com.blinkbox.books.rabbitmq.RabbitMqConfig
import com.typesafe.config.Config
import com.zaxxer.hikari
import spray.http.{BasicHttpCredentials, HttpCredentials}

import scala.collection.convert.Wrappers
import scala.concurrent.duration._

case class AppConfig(
    service: ApiConfig,
    swagger: SwaggerConfig,
    db: DatabaseConfig,
    hikari: HikariConfig,
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
    refreshTokenLifetimeExtension: FiniteDuration)

case class SsoConfig(
    host: String,
    port: Int,
    version: String,
    credentials: HttpCredentials,
    timeout: FiniteDuration,
    keyStore: String) {

  require(Files.exists(Paths.get(keyStore)), s"SSO key-store file not found: $keyStore")
}

case class HikariConfig(
    dataSourceClassName: String,
    autoCommit: Option[Boolean],
    readOnly: Option[Boolean],
    transactionIsolation: Option[String],
    catalog: Option[String],
    connectionTimeout: Option[FiniteDuration],
    idleTimeout: Option[FiniteDuration],
    maxLifetime: Option[FiniteDuration],
    leakDetectionThreshold: Option[FiniteDuration],
    initializationFailFast: Option[Boolean],
    jdbc4ConnectionTest: Option[Boolean],
    connectionTestQuery: Option[String],
    connectionInitSql: Option[String],
    minimumIdle: Option[Int],
    maximumPoolSize: Option[Int],
    poolName: Option[String],
    dataSourceProperties: Properties
) {
  def get: hikari.HikariConfig = {
    val c = new hikari.HikariConfig()

    c.setDataSourceClassName(dataSourceClassName)

    autoCommit.foreach(c.setAutoCommit)
    readOnly.foreach(c.setReadOnly)
    transactionIsolation.foreach(c.setTransactionIsolation)
    catalog.foreach(c.setCatalog)
    connectionTimeout.map(_.toMillis).foreach(c.setConnectionTimeout)
    idleTimeout.map(_.toMillis).foreach(c.setIdleTimeout)
    maxLifetime.map(_.toMillis).foreach(c.setMaxLifetime)
    leakDetectionThreshold.map(_.toMillis).foreach(c.setLeakDetectionThreshold)
    initializationFailFast.foreach(c.setInitializationFailFast)
    jdbc4ConnectionTest.foreach(c.setJdbc4ConnectionTest)
    connectionTestQuery.foreach(c.setConnectionTestQuery)
    connectionInitSql.foreach(c.setConnectionInitSql)
    minimumIdle.foreach(c.setMinimumIdle)
    maximumPoolSize.foreach(c.setMaximumPoolSize)
    poolName.foreach(c.setPoolName)

    c.setDataSourceProperties(dataSourceProperties)

    c
  }
}

object HikariConfig {
  def apply(config: Config, prefix: String): HikariConfig = {
    val dataSourceProps = new Properties()

    Wrappers.JSetWrapper(config.getConfig(s"$prefix.dataSource").entrySet()) foreach { entry =>
      dataSourceProps.setProperty(entry.getKey, entry.getValue.render)
    }

    HikariConfig(
      config.getString(s"$prefix.dataSourceClassName"),
      config.getBooleanOption(s"$prefix.autoCommit"),
      config.getBooleanOption(s"$prefix.readOnly"),
      config.getStringOption(s"$prefix.transactionIsolation"),
      config.getStringOption(s"$prefix.catalog"),
      config.getFiniteDurationOption(s"$prefix.connectionTimeout"),
      config.getFiniteDurationOption(s"$prefix.idleTimeout"),
      config.getFiniteDurationOption(s"$prefix.maxLifetime"),
      config.getFiniteDurationOption(s"$prefix.leakDetectionThreshold"),
      config.getBooleanOption(s"$prefix.initializationFailFast"),
      config.getBooleanOption(s"$prefix.jdbc4ConnectionTest"),
      config.getStringOption(s"$prefix.connectionTestQuery"),
      config.getStringOption(s"$prefix.connectionInitSql"),
      config.getIntOption(s"$prefix.minimumIdle"),
      config.getIntOption(s"$prefix.maximumPoolSize"),
      config.getStringOption(s"$prefix.poolName"),
      dataSourceProps
    )
  }
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
    refreshTokenLifetimeExtension = config.getFiniteDuration(s"$prefix.refreshTokenLifetimeExtension")
  )
}

object SsoConfig {
  def apply(config: Config): SsoConfig = SsoConfig(
    host = config.getString("sso.host"),
    port = config.getInt("sso.port"),
    version = config.getString("sso.version"),
    credentials = BasicHttpCredentials(config.getString("sso.credentials.username"), config.getString("sso.credentials.password")),
    timeout = config.getFiniteDuration("sso.timeout"),
    keyStore = config.getString("sso.keyStore")
  )
}

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.auth.api.public"),
    SwaggerConfig(config, 1),
    DatabaseConfig(config, "service.auth.db"),
    HikariConfig(config, "service.auth.hikari"),
    RabbitMqConfig(config),
    AuthClientConfig(config),
    SsoConfig(config),
    AuthServerConfig(config, "service.auth"))

  lazy val default: AppConfig = AppConfig((new Configuration {}).config)
}
