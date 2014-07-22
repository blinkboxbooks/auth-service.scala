package com.blinkbox.books.auth.server

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import org.joda.time.DateTime

import org.json4s.ext.{JodaTimeSerializers, EnumNameSerializer}
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray._
import com.blinkbox.books.spray.Directives
import com.wordnik.swagger.annotations._
import org.slf4j.LoggerFactory
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http.StatusCodes._
import spray.http.{HttpChallenge}
import spray.routing._
import org.json4s._
import spray.httpx.unmarshalling.FormDataUnmarshallers
import com.blinkbox.books.auth.User
import spray.httpx.Json4sJacksonSupport
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

class AuthApi(config: ApiConfig, userService: AuthService, authenticator: ContextAuthenticator[User])(implicit val actorRefFactory: ActorRefFactory)
  extends AuthRoutes with Directives with FormDataUnmarshallers with Json4sJacksonSupport {

  implicit val log = LoggerFactory.getLogger(classOf[AuthApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout: Timeout = config.timeout

  val clientInfoSerializer = FieldSerializer[ClientInfo](
    serializer = {
      case ("last_used_date", d) => Some("last_used_date", d.asInstanceOf[DateTime].toString("yyyy-MM-dd"))
    }
  )

  implicit def json4sJacksonFormats: Formats = (DefaultFormats + ZuulRequestExceptionSerializer +
    new EnumNameSerializer(RefreshTokenStatus) + clientInfoSerializer) ++ JodaTimeSerializers.all

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

  val registerUser: Route = post {
    path("oauth2" / "token") {
      formField('grant_type ! "urn:blinkbox:oauth:grant-type:registration") {
        formFields('first_name, 'last_name, 'username, 'password, 'accepted_terms_and_conditions.as[Boolean], 'allow_marketing_communications.as[Boolean], 'client_name.?, 'client_brand.?, 'client_model.?, 'client_os.?).as(UserRegistration) { registration =>
          extract(_.request.clientIP) { clientIP =>
            onSuccess(userService.registerUser(registration, clientIP)) { tokenInfo =>
              uncacheable(OK, tokenInfo)
            }
          }
        }
      }
    }
  }

  val authenticate: Route = post {
    path("oauth2" / "token") {
      formField('grant_type ! "password") {
        formFields('username, 'password, 'client_id.?, 'client_secret.?).as(PasswordCredentials) { credentials =>
          extract(_.request.clientIP) { clientIP =>
            onSuccess(userService.authenticate(credentials, clientIP)) { tokenInfo =>
              uncacheable(OK, tokenInfo)
            }
          }
        }
      }
    }
  }

  val refreshAccessToken: Route = post {
    path("oauth2" / "token") {
      formField('grant_type ! "refresh_token") {
        formFields('refresh_token, 'client_id.?, 'client_secret.?).as(RefreshTokenCredentials) { credentials =>
          onSuccess(userService.refreshAccessToken(credentials)) { tokenInfo =>
            uncacheable(OK, tokenInfo)
          }
        }
      }
    }
  }

  val querySession: Route = get {
    path("session") {
      authenticate(authenticator) { implicit user =>
        onSuccess(userService.querySession) { sessionInfo =>
          uncacheable(OK, sessionInfo)
        }
      }
    }
  }

  val registerClient: Route = post {
    path("clients") {
      authenticate(authenticator) { implicit user =>
        formFields('client_name, 'client_brand, 'client_model, 'client_os).as(ClientRegistration) { registration =>
          onSuccess(userService.registerClient(registration)) { client =>
            uncacheable(OK, client)
          }
        }
      }
    }
  }

  val listClients: Route = get {
    path("clients") {
      authenticate(authenticator) { implicit user =>
        onSuccess(userService.listClients) { clients =>
          uncacheable(OK, clients)
        }
      }
    }
  }

  val getClientById: Route = get {
    path("clients" / IntNumber) { id =>
      authenticate(authenticator) { implicit user =>
        onSuccess(userService.getClientById(id)) {
          case Some(client) => uncacheable(OK, client)
          case None => complete(NotFound, None)
        }
      }
    }
  }

  val updateClient: Route = patch {
    path("clients" / IntNumber) { id =>
      authenticate(authenticator) { implicit user =>
        formFields('client_name.?, 'client_brand.?, 'client_model.?, 'client_os.?).as(ClientPatch) { patch =>
          onSuccess(userService.updateClient(id, patch)) {
            case Some(client) => uncacheable(OK, client)
            case None => complete(NotFound, None)
          }
        }
      }
    }
  }

  val deleteClient: Route = delete {
    path("clients" / IntNumber) { id =>
      authenticate(authenticator) { implicit user =>
        onSuccess(userService.deleteClient(id)) { clientOpt =>
          clientOpt.fold(complete(NotFound, None))(_ => complete(OK, None))
        }
      }
    }
  }

  val revokeRefreshToken: Route = post {
    path("tokens" / "revoke") {
      formFields('refresh_token) { token =>
        onSuccess(userService.revokeRefreshToken(token)) { _ =>
          complete(OK, None)
        }
      }
    }
  }

  val getUserInfo: Route = get {
    path("users" / IntNumber) { userId =>
      authenticate(authenticator) { implicit user =>
        if (user.id != userId)
          complete(NotFound, None)
        else
          onSuccess(userService.getUserInfo) { info =>
            info.fold(complete(NotFound, None))(i => uncacheable(OK, i))
          }
      }
    }
  }

  val updateUserInfo: Route = patch {
    path("users" / IntNumber) { userId =>
      authenticate(authenticator) { implicit user =>
        if (user.id != userId)
          complete(NotFound, None)
        else
          formFields('first_name.?, 'last_name.?, 'username.?, 'allow_marketing_communications.?, 'accepted_terms_and_conditions.?).as(UserPatch) { patch =>
            onSuccess(userService.updateUser(userId, patch)) { info =>
              info.fold(complete(NotFound, None))(i => uncacheable(OK, i))
            }
        }
      }
    }
  }

//
//  val list: Route = get {
//    pathEnd {
//      paged(defaultCount = 25) { page =>
//        //authenticate(authenticator) { implicit user =>
//          onSuccess(userService.list(page)) { users =>
//            uncacheable(users)
//          }
//        }
//      }
//    }
//  }
//
//  val getById: Route = get {
//    path(IntNumber) { id =>
//      authenticate(authenticator) { implicit user =>
//        onSuccess(userService.getById(id)) {
//          case Some(user) => uncacheable(user)
//          case None => complete(NotFound, None)
//        }
//      }
//    }
//  }
//
//  val updateById: Route = (patch | put) { // note: put is required because level 3 don't support patch
//    path(IntNumber) { id =>
//      authenticate(authenticator) { implicit user =>
//        entity(as[UserPatch]) { patch =>
//          onSuccess(userService.update(id, patch)) {
//            case Some(user) => complete(user)
//            case None => complete(NotFound, None)
//          }
//        }
//      }
//    }
//  }
//
//  val deleteById: Route = delete {
//    path(IntNumber) { id =>
//      authenticate(authenticator) { implicit user =>
//        onSuccess(userService.delete(id)) { _ =>
//          complete(OK, None)
//        }
//      }
//    }
//  }

  val routes: Route = monitor() {
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        //rawPathPrefix(PathMatcher[HNil](config.externalUrl.path, HNil)) {
        //respondWithHeader(RawHeader("Vary", "Accept, Accept-Encoding")) {
        querySession ~ refreshAccessToken ~ authenticate ~ registerUser ~ registerClient ~ listClients ~ getClientById ~
          updateClient ~ deleteClient ~ revokeRefreshToken ~ getUserInfo ~ updateUserInfo
        //}
        //}
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
  }

  def rejectionHandler = RejectionHandler {
    case MissingFormFieldRejection(field) :: _ => complete(BadRequest, ZuulRequestException(s"Missing field: $field", InvalidRequest, None))
    case MalformedFormFieldRejection(field, message, _) :: _ => complete(BadRequest, ZuulRequestException(s"$field: $message", InvalidRequest, None))
    case ValidationRejection(message, _) :: _ => complete(BadRequest, ZuulRequestException(message, InvalidRequest, None))
  }
}
