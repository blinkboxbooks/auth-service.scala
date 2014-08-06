package com.blinkbox.books.auth.server.cake

import akka.actor.ActorSystem
import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.SSO
import com.blinkbox.books.auth.server.{AppConfig, PasswordHasher}
import com.blinkbox.books.auth.server.sso.SSO
import com.blinkbox.books.slick.DatabaseTypes
import com.blinkbox.books.time._
import spray.routing.Route
import spray.routing.authentication.ContextAuthenticator

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile

trait ConfigComponent {
  def config: AppConfig
}

trait AsyncComponent {
  def actorSystem: ActorSystem
  def executionContext: ExecutionContext

  implicit lazy val ec = executionContext
  implicit lazy val system = actorSystem
}

trait EventsComponent {
  def publisher: Publisher
}

trait DatabaseComponent[DbTypes <: DatabaseTypes] {
  def driver: DbTypes#Profile
  def db: DbTypes#Database
  def tables: ZuulTables
}

trait PasswordHasherComponent {
  def passwordHasher: PasswordHasher
}

trait RepositoriesComponent[Profile <: JdbcProfile] {
  def authRepository: AuthRepository[Profile]
  def userRepository: UserRepository[Profile]
  def clientRepository: ClientRepository[Profile]
}

trait GeoIPComponent {
  def geoIp: GeoIP
}

trait AuthServiceComponent {
  def authService: AuthService
}

trait ClientServiceComponent {
  def clientService: ClientService
}

trait UserServiceComponent {
  def userService: UserService
}

trait RegistrationServiceComponent {
  def registrationService: RegistrationService
}

trait SSOComponent {
  def sso: SSO
}

trait ApiComponent {
  def authenticator: ContextAuthenticator[User]
  def zuulRoutes: Route
  def swaggerRoutes: Route
}

trait WebComponent {
  this: AsyncComponent with ConfigComponent =>
}
