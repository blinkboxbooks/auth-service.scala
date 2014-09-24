package com.blinkbox.books.auth.server.cake

import akka.actor.ActorSystem
import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.Publisher
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.Sso
import com.blinkbox.books.auth.server.{AppConfig, PasswordHasher, TokenBuilder}
import com.blinkbox.books.slick
import spray.routing.Route
import spray.routing.authentication.ContextAuthenticator

import scala.concurrent.ExecutionContext

trait ConfigComponent {
  def config: AppConfig
}

trait AsyncComponent {
  def actorSystem: ActorSystem
  def rabbitExecutionContext: ExecutionContext
  def serviceExecutionContext: ExecutionContext
  def apiExecutionContext: ExecutionContext
  def ssoClientExecutionContext: ExecutionContext

  implicit lazy val as = actorSystem

  def withServiceContext[T](f: ExecutionContext => T) = f(serviceExecutionContext)
  def withRabbitContext[T](f: ExecutionContext => T) = f(rabbitExecutionContext)
  def withSsoClientContext[T](f: ExecutionContext => T) = f(ssoClientExecutionContext)
  def withApiContext[T](f: ExecutionContext => T) = f(apiExecutionContext)
}

trait EventsComponent {
  def publisher: Publisher
}

trait DatabaseComponent extends slick.DatabaseComponent {
  type Tables = ZuulTables[DB.Profile]
}

trait TokenBuilderComponent {
  def tokenBuilder: TokenBuilder
}

trait RepositoriesComponent extends slick.RepositoriesComponent {
  this: DatabaseComponent =>
  def authRepository: AuthRepository[DB.Profile]
  def userRepository: UserRepository[DB.Profile]
  def clientRepository: ClientRepository[DB.Profile]
  def roleRepository: RoleRepository[DB.Profile]
}

trait GeoIPComponent {
  def geoIp: GeoIP
}

trait AuthServiceComponent {
  def sessionService: SessionService
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

trait PasswordAuthenticationServiceComponent {
  def passwordAuthenticationService: PasswordAuthenticationService
}

trait RefreshTokenServiceComponent {
  def refreshTokenService: RefreshTokenService
}

trait SsoSyncComponent {
  def ssoSync: SsoSyncService
}

trait PasswordUpdateServiceComponent {
  def passwordUpdateService: PasswordUpdateService
}

trait AdminUserServiceComponent {
  def adminUserService: AdminUserService
}

trait SsoComponent {
  def sso: Sso
}

trait ApiComponent {
  def authenticator: ContextAuthenticator[User]
  def routes: Route
}

trait WebComponent {
  this: AsyncComponent with ConfigComponent =>
}
