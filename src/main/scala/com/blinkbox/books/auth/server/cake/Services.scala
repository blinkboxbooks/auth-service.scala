package com.blinkbox.books.auth.server.cake

import akka.actor.ActorSystem
import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.SSO
import com.blinkbox.books.auth.server.{AppConfig, PasswordHasher}
import spray.routing.Route
import spray.routing.authentication.ContextAuthenticator

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

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

trait DBTypes {
  type Profile <: JdbcProfile
  type ConstraintException <: Throwable
  type Database = Profile#Backend#Database
  val constraintExceptionTag: ClassTag[ConstraintException]
}

trait DatabaseComponent {
  type Types <: DBTypes
  type Tables = ZuulTables[Types#Profile]

  def driver: Types#Profile
  def db: Types#Database
  def tables: ZuulTables[Types#Profile]

  implicit val constraintExceptionTag: ClassTag[Types#ConstraintException]
}

trait PasswordHasherComponent {
  def passwordHasher: PasswordHasher
}

trait RepositoriesComponent {
  this: DatabaseComponent =>
  def authRepository: AuthRepository[Types#Profile]
  def userRepository: UserRepository[Types#Profile]
  def clientRepository: ClientRepository[Types#Profile]
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
