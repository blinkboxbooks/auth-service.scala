package com.blinkbox.books.auth.server

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.auth.Elevation._
import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.{SsoPasswordResetToken, SsoUnknownException}
import com.blinkbox.books.auth.{Constraints, User, UserRole}
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray.AuthDirectives._
import com.blinkbox.books.spray.{Directives, _}
import org.slf4j.LoggerFactory
import spray.http.HttpEntity
import spray.http.HttpHeaders.{RawHeader, `WWW-Authenticate`}
import spray.http.StatusCodes._
import spray.httpx.unmarshalling.{Deserializer, FormDataUnmarshallers}
import spray.routing._

class AuthApi(
    config: ApiConfig,
    userService: UserService,
    clientService: ClientService,
    sessionService: SessionService,
    registrationService: RegistrationService,
    passwordAuthenticationService: PasswordAuthenticationService,
    refreshTokenService: RefreshTokenService,
    passwordUpdateService: PasswordUpdateService,
    adminUserService: AdminUserService,
    authenticator: ElevatedContextAuthenticator[User])(implicit val actorRefFactory: ActorRefFactory)
  extends HttpService with Directives with FormDataUnmarshallers {

  implicit val log = LoggerFactory.getLogger(classOf[AuthApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout: Timeout = config.timeout

  import com.blinkbox.books.auth.server.Serialization._

  val UserId = IntNumber.map(data.UserId(_))
  val ClientId = IntNumber.map(data.ClientId(_))
  val RefreshTokenId = IntNumber.map(data.RefreshTokenId(_))

  val resetTokenDeserializer = Deserializer.fromFunction2Converter((s: String) => SsoPasswordResetToken(s))
  val userIdDeserializer = Deserializer.fromFunction2Converter((s: String) => data.UserId(s.toInt))

  val withSearchCriteria = formFields('username).as[SearchCriteria](UsernameSearch) |
    formField('first_name, 'last_name).as[SearchCriteria](NameSearch) |
    formFields('user_id.as(userIdDeserializer)).as[SearchCriteria](IdSearch)

  val registerUser: Route = formField('grant_type ! "urn:blinkbox:oauth:grant-type:registration") {
    formFields('first_name, 'last_name, 'username, 'password, 'accepted_terms_and_conditions.as[Boolean], 'allow_marketing_communications.as[Boolean], 'client_name.?, 'client_brand.?, 'client_model.?, 'client_os.?).as(UserRegistration) { registration =>
      extract(_.request.clientIP) { clientIP =>
        onSuccess(registrationService.registerUser(registration, clientIP)) { tokenInfo =>
          uncacheable(OK, tokenInfo)
        }
      }
    }
  }

  val authenticate: Route = formField('grant_type ! "password") {
    formFields('username, 'password, 'client_id.?, 'client_secret.?).as(PasswordCredentials) { credentials =>
      extract(_.request.clientIP) { clientIP =>
        onSuccess(passwordAuthenticationService.authenticate(credentials, clientIP)) { tokenInfo =>
          uncacheable(OK, tokenInfo)
        }
      }
    }
  }

  val refreshAccessToken: Route = formField('grant_type ! "refresh_token") {
    formFields('refresh_token, 'client_id.?, 'client_secret.?).as(RefreshTokenCredentials) { credentials =>
      onSuccess(refreshTokenService.refreshAccessToken(credentials)) { tokenInfo =>
        uncacheable(OK, tokenInfo)
      }
    }
  }

  val resetPassword: Route = formField('grant_type ! "urn:blinkbox:oauth:grant-type:password-reset-token") {
    formFields('password_reset_token.as(resetTokenDeserializer), 'password, 'client_id.?, 'client_secret.?).as(ResetTokenCredentials) { credentials =>
      onSuccess(passwordUpdateService.resetPassword(credentials)) { tokenInfo =>
        uncacheable(OK, tokenInfo)
      }
    }
  }

  val oAuthToken: Route = post {
    path("oauth2" / "token") {
      registerUser ~ authenticate ~ refreshAccessToken ~ resetPassword
    }
  }

  val getUserInfo: Route = get {
    path("users" / UserId) { userId =>
      authenticate(authenticator) { implicit user =>
        if (user.id != userId.value)
          complete(NotFound, None)
        else
          onSuccess(userService.getUserInfo()) { info =>
            info.fold(complete(NotFound, None))(i => uncacheable(OK, i))
          }
      }
    }
  }

  val updateUserInfo: Route = patch {
    path("users" / UserId) { userId =>
      authenticate(authenticator) { implicit user =>
        if (user.id != userId.value)
          complete(NotFound, None)
        else
          formFields('first_name.?, 'last_name.?, 'username.?, 'allow_marketing_communications.?, 'accepted_terms_and_conditions.?).as(UserPatch) { patch =>
            onSuccess(userService.updateUser(patch)) { info =>
              info.fold(complete(NotFound, None))(i => uncacheable(OK, i))
            }
          }
      }
    }
  }

  val querySession: Route = get {
    path("session") {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        onSuccess(sessionService.querySession) { sessionInfo =>
          uncacheable(OK, sessionInfo)
        }
      }
    }
  }

  val renewSession: Route = post {
    path("session") {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        onSuccess(sessionService.extendSession) { sessionInfo =>
          uncacheable(OK, sessionInfo)
        }
      }
    }
  }

  val registerClient: Route = post {
    path("clients") {
      authenticate(authenticator) { implicit user =>
        formFields('client_name, 'client_brand, 'client_model, 'client_os).as(ClientRegistration) { registration =>
          onSuccess(clientService.registerClient(registration)) { client =>
            uncacheable(OK, client)
          }
        }
      }
    }
  }

  val listClients: Route = get {
    path("clients") {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        onSuccess(clientService.listClients) { clients =>
          uncacheable(OK, clients)
        }
      }
    }
  }

  val getClientById: Route = get {
    path("clients" / ClientId) { id =>
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        onSuccess(clientService.getClientById(id)) {
          case Some(client) => uncacheable(OK, client)
          case None => complete(NotFound, None)
        }
      }
    }
  }

  val updateClient: Route = patch {
    path("clients" / ClientId) { id =>
      authenticate(authenticator) { implicit user =>
        formFields('client_name.?, 'client_brand.?, 'client_model.?, 'client_os.?).as(ClientPatch) { patch =>
          onSuccess(clientService.updateClient(id, patch)) {
            case Some(client) => uncacheable(OK, client)
            case None => complete(NotFound, None)
          }
        }
      }
    }
  }

  val deleteClient: Route = delete {
    path("clients" / ClientId) { id =>
      authenticate(authenticator) { implicit user =>
        onSuccess(clientService.deleteClient(id)) { clientOpt =>
          clientOpt.fold(complete(NotFound, None))(_ => complete(OK, None))
        }
      }
    }
  }

  val revokeRefreshToken: Route = post {
    path("tokens" / "revoke") {
      formFields('refresh_token) { token =>
        onSuccess(refreshTokenService.revokeRefreshToken(token)) { _ =>
          complete(OK, None)
        }
      }
    }
  }

  val passwordChange: Route = post {
    path("password" / "change") {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        formFields('old_password, 'new_password) { (oldPassword, newPassword) =>
          onSuccess(passwordUpdateService.updatePassword(oldPassword, newPassword)) { _ =>
            complete(OK, None)
          }
        }
      }
    }
  }

  val generatePasswordResetToken: Route = post {
    path("password" / "reset") {
      formFields('username) { username =>
        onSuccess(passwordUpdateService.generatePasswordResetToken(username)) { _ =>
          complete(OK, None)
        }
      }
    }
  }

  val validatePasswordResetToken: Route = post {
    path("password" / "reset" / "validate-token") {
      formFields('password_reset_token) { token =>
        onSuccess(passwordUpdateService.validatePasswordResetToken(SsoPasswordResetToken(token))) { _ =>
          complete(OK, None)
        }
      }
    }
  }

  val admin: Route = pathPrefix("admin") {
    authenticateAndAuthorize(authenticator, Constraints.hasAnyRole(UserRole.CustomerServicesManager, UserRole.CustomerServicesRep)) { _ =>
      path("users") {
        withSearchCriteria { criteria =>
          onSuccess(adminUserService.userSearch(criteria)) { info =>
            complete(OK, info)
          }
        }
      } ~
      path("users" / UserId) { id =>
        onSuccess(adminUserService.userDetails(id)) { info =>
          complete(OK, info)
        }
      }
    }
  }

  val routes: Route = rootPath(config.localUrl.path) {
    monitor() {
      handleExceptions(exceptionHandler) {
        handleRejections(rejectionHandler) {
          querySession ~ renewSession ~ oAuthToken ~ registerClient ~ listClients ~ getClientById ~
            updateClient ~ deleteClient ~ revokeRefreshToken ~ getUserInfo ~ updateUserInfo ~ passwordChange ~
            validatePasswordResetToken ~ generatePasswordResetToken ~ admin
        }
      }
    }
  }

  def exceptionHandler = ExceptionHandler {
    case e: ZuulRequestException => complete(BadRequest, e)
    case e: ZuulAuthorizationException =>
      val params =
        Some("error" -> ZuulAuthorizationErrorCode.toString(e.code)) ::
        e.reason.map(r => "error_reason" -> ZuulAuthorizationErrorReason.toString(r)) ::
        Some("error_description" -> e.message) :: Nil
      val challenge = new BearerHttpChallenge(params.flatten.toMap)
      respondWithHeader(`WWW-Authenticate`(challenge)) { complete(Unauthorized, None) }
    case ZuulTooManyRequestException(_, retryAfter) =>
      respondWithHeader(RawHeader("Retry-After", retryAfter.toSeconds.toString)) {
        complete(TooManyRequests, HttpEntity.Empty)
      }
    case SsoUnknownException(e) =>
      log.error("Unknown SSO error", e)
      complete(InternalServerError, HttpEntity.Empty)
    case ZuulUnknownException(msg, inner) =>
      log.error(s"Unknown error: $msg", inner)
      complete(InternalServerError, HttpEntity.Empty)
  }

  def rejectionHandler = RejectionHandler {
    case MissingFormFieldRejection(field) :: _ => complete(BadRequest, ZuulRequestException(s"Missing field: $field", InvalidRequest, None))
    case MalformedFormFieldRejection(field, message, _) :: _ => complete(BadRequest, ZuulRequestException(s"$field: $message", InvalidRequest, None))
    case ValidationRejection(message, _) :: _ => complete(BadRequest, ZuulRequestException(message, InvalidRequest, None))
  }
}
