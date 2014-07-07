package com.blinkbox.books.agora

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.blinkbox.books.auth.User
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.logging.DiagnosticExecutionContext
import com.blinkbox.books.spray._
import com.blinkbox.books.spray.{Directives, Page}
import com.blinkbox.books.spray.JsonFormats._
import com.blinkbox.books.spray.v1._
import com.wordnik.swagger.annotations._
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.util.control.NonFatal
import shapeless.HNil
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.http.{HttpCharsets, HttpEntity, Uri}
import spray.routing._
import spray.routing.authentication._
import spray.util.LoggingContext
import org.json4s._
import spray.httpx.marshalling.Marshaller
import org.json4s.jackson.Serialization
import spray.http.MediaTypes._
import spray.httpx.unmarshalling.{Unmarshaller, FormDataUnmarshallers}
import com.blinkbox.books.agora.Registration
import com.blinkbox.books.auth.User
import com.blinkbox.books.agora.Registration
import com.blinkbox.books.auth.User
import java.lang.reflect.InvocationTargetException
import spray.httpx.{Json4sJacksonSupport, Json4sSupport}

@Api(value = "/user", description = "An API for managing widgets.", protocols = "https",
     produces = "application/vnd.blinkboxbooks.data.v1+json", consumes = "application/vnd.blinkboxbooks.data.v1+json")
trait UserRoutes extends HttpService {

  @ApiOperation(position = 0, httpMethod = "POST", response = classOf[User], value = "Creates a user")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "user", required = true, dataType = "NewUser", paramType = "body", value = "The user to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "The user details were incomplete or invalid"),
    new ApiResponse(code = 401, message = "We're not sure who you are")
  ))
  def register: Route

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

class UserApi(config: ApiConfig, userService: UserService)(implicit val actorRefFactory: ActorRefFactory)
  extends UserRoutes with Directives with FormDataUnmarshallers with Json4sJacksonSupport {

  implicit val log = LoggerFactory.getLogger(classOf[UserApi])
  implicit val executionContext = DiagnosticExecutionContext(actorRefFactory.dispatcher)
  implicit val timeout: Timeout = config.timeout


  implicit def json4sJacksonFormats: Formats = DefaultFormats

//  implicit def version1JsonUnmarshaller[T: Manifest]: Unmarshaller[T] =
//    Unmarshaller[T](`application/vnd.blinkboxbooks.data.v1+json`) {
//      case x: HttpEntity.NonEmpty =>
//        try Serialization.read[T](x.asString(defaultCharset = HttpCharsets.`UTF-8`))
//        catch {
//          case MappingException("unknown error", ite: InvocationTargetException) => throw ite.getCause
//        }
//    }



  //implicit def version1JsonMarshaller[T <: AnyRef]: Marshaller[T] = Marshaller.delegate[T, String](`application/json`)(Serialization.write(_))

//  implicit def formats: Formats = DefaultFormats
//  implicit def marshaller[T <: AnyRef] = Marshaller.delegate[T, String](`application/json`)

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

  val register: Route = post {
    path("oauth2" / "token") {
      formField('grant_type ! "urn:blinkbox:oauth:grant-type:registration") {
        formFields('first_name, 'last_name, 'username, 'password, 'accepted_terms_and_conditions.as[Boolean], 'allow_marketing_communications.as[Boolean], 'client_name.?, 'client_brand.?, 'client_model.?, 'client_os.?).as(Registration) { registration =>
          onComplete(userService.create(registration)) {
            case scala.util.Success(tokenInfo) => complete(OK, tokenInfo)
            case scala.util.Failure(e: UserAlreadyExists) => complete(BadRequest, OAuthError("username_already_taken", e.getMessage))
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
    //rawPathPrefix(PathMatcher[HNil](config.externalUrl.path, HNil)) {
      //respondWithHeader(RawHeader("Vary", "Accept, Accept-Encoding")) {
        register
      //}
    //}
  }
}
