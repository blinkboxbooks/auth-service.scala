package com.blinkbox.books.auth.server

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.services._
import com.blinkbox.books.auth.server.sso.SSOUnknownException
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray._
import com.blinkbox.books.spray.Directives
import com.wordnik.swagger.annotations._
import org.slf4j.LoggerFactory
import spray.http.HttpHeaders.{RawHeader, `WWW-Authenticate`}
import spray.http.StatusCodes._
import spray.http.{HttpEntity, HttpChallenge}
import spray.routing._
import spray.httpx.unmarshalling.FormDataUnmarshallers
import com.blinkbox.books.auth.User
import spray.routing.authentication.ContextAuthenticator

@Api(value = "/user", description = "An API for managing widgets.", protocols = "https",
     produces = "application/vnd.blinkboxbooks.data.v1+json", consumes = "application/vnd.blinkboxbooks.data.v1+json")
trait AuthRoutes extends HttpService {

  @ApiOperation(position = 0, httpMethod = "POST", response = classOf[User], value = "Creates a user")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "user", required = true, dataType = "NewUser", paramType = "body", value = "The user to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "The user details were incomplete or invalid"),
    new ApiResponse(code = 401, message = "We're not sure who you are")
  ))
  def registerUser: Route

//  @ApiOperation(position = 1, httpMethod = "GET", response = classOf[User], responseContainer = "ListPage", value = "Gets a list of users")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "offset", required = false, dataType = "Int", paramType = "query", value = "The offset into the list at which to start returning results"),
//    new ApiImplicitParam(name = "count", required = false, dataType = "Int", paramType = "query", value = "The maximum number of results to return")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 400, message = "offset is less than zero, or count is less than one"),
//    new ApiResponse(code = 401, message = "We're not sure who you are")
//  ))
//  def list: Route
//
//  @ApiOperation(position = 2, httpMethod = "GET", response = classOf[User], value = "Gets a user by id")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "id", required = true, dataType = "String", paramType = "path", value = "The user id")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 401, message = "We're not sure who you are"),
//    new ApiResponse(code = 403, message = "You're not allowed to access that user"),
//    new ApiResponse(code = 404, message = "That user doesn't exist")
//  ))
//  def getById: Route
//
//  @ApiOperation(position = 3, httpMethod = "PATCH", response = classOf[User], value = "Updates a user")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "id", required = true, dataType = "String", paramType = "path", value = "The user id"),
//    new ApiImplicitParam(name = "user", required = true, dataType = "UserPatch", paramType = "body", value = "The updated user details")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 400, message = "The user details were invalid"),
//    new ApiResponse(code = 401, message = "We're not sure who you are"),
//    new ApiResponse(code = 404, message = "That user doesn't exist")
//  ))
//  def updateById: Route
//
//  @ApiOperation(position = 4, httpMethod = "DELETE", response = classOf[User], value = "Deletes a user")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "id", required = true, dataType = "String", paramType = "path", value = "The user id")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 401, message = "We're not sure who you are"),
//    new ApiResponse(code = 404, message = "That user doesn't exist")
//  ))
//  def deleteById: Route
}

class AuthApi(
    config: ApiConfig,
    userService: UserService,
    clientService: ClientService,
    sessionService: SessionService,
    registrationService: RegistrationService,
    passwordAuthenticationService: PasswordAuthenticationService,
    refreshTokenService: RefreshTokenService,
    passwordUpdateService: PasswordUpdateService,
    authenticator: ContextAuthenticator[User])(implicit val actorRefFactory: ActorRefFactory)
  extends AuthRoutes with Directives with FormDataUnmarshallers {

  implicit val log = LoggerFactory.getLogger(classOf[AuthApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout: Timeout = config.timeout

  import Serialization._

  val UserId = IntNumber.map(data.UserId(_))
  val ClientId = IntNumber.map(data.ClientId(_))
  val RefreshTokenId = IntNumber.map(data.RefreshTokenId(_))

//  first_name (required)
//  last_name (required)
//  username (required)
//  password (required)
//  accepted_terms_and_conditions (required)
//  allow_marketing_communications (required)
//  client_name (required to register client simultaneously)
//  client_brand (required to register client simultaneously)
//  client_model (required to register client simultaneously)
//  client_os(required to register client simultaneously)

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

  val oAuthToken: Route = post {
    path("oauth2" / "token") {
      registerUser ~ authenticate ~ refreshAccessToken
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
      authenticate(authenticator) { implicit user =>
        onSuccess(sessionService.querySession) { sessionInfo =>
          uncacheable(OK, sessionInfo)
        }
      }
    }
  }

  val renewSession: Route = post {
    path("session") {
      authenticate(authenticator) { implicit user =>
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
      authenticate(authenticator) { implicit user =>
        onSuccess(clientService.listClients) { clients =>
          uncacheable(OK, clients)
        }
      }
    }
  }

  val getClientById: Route = get {
    path("clients" / ClientId) { id =>
      authenticate(authenticator) { implicit user =>
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
      authenticate(authenticator) { implicit user =>
        formFields('old_password, 'new_password) { (oldPassword, newPassword) =>
          onSuccess(passwordUpdateService.updatePassword(oldPassword, newPassword)) { _ =>
            // TODO: Check that this endpoint should return an empty response
            complete(OK, None)
          }
        }
      }
    }
  }

  val routes: Route = monitor() {
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        querySession ~ renewSession ~ oAuthToken ~ registerClient ~ listClients ~ getClientById ~
          updateClient ~ deleteClient ~ revokeRefreshToken ~ getUserInfo ~ updateUserInfo ~ passwordChange
      }
    }
  }

  def exceptionHandler = ExceptionHandler {
    case e: ZuulRequestException => complete(BadRequest, e)
    case e: ZuulAuthorizationException =>
      val commonChallenges = HttpChallenge("Bearer realm", "blinkbox.com") ::
        HttpChallenge("error", ZuulAuthorizationErrorCode.toString(e.code)) ::
        HttpChallenge("error_description", e.message) :: Nil

      val challenges = e.reason.fold(commonChallenges) {
        r => HttpChallenge("error_reason", ZuulAuthorizationErrorReason.toString(r)) :: commonChallenges
      }

      respondWithHeader(`WWW-Authenticate`.apply(challenges)) { complete(Unauthorized, None) }
    case ZuulTooManyRequestException(_, retryAfter) =>
      respondWithHeader(RawHeader("Retry-After", retryAfter.toSeconds.toString)) {
        complete(TooManyRequests, HttpEntity.Empty)
      }
    case SSOUnknownException(e) =>
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
