package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import spray.http._
import spray.httpx.unmarshalling.FormDataUnmarshallers
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest

class RegistrationSpecs extends FlatSpec with Matchers with ScalatestRouteTest with HttpService with BeforeAndAfterEach
  with FormDataUnmarshallers {

  def actorRefFactory = system

  def newEnv = new DefaultH2ApiTestEnv {
    implicit val system = RegistrationSpecs.this.system
  }

  var env: DefaultH2ApiTestEnv = _
  var route: Route = _

  override def beforeEach() {
    env = newEnv
    route = env.route
  }

  val regDataSimple = Map(
    "grant_type" -> "urn:blinkbox:oauth:grant-type:registration",
    "first_name" -> "First",
    "last_name" -> "Last",
    "username" -> "user@domain.test",
    "password" -> "passw0rd",
    "accepted_terms_and_conditions" -> "true",
    "allow_marketing_communications" -> "true"
  )

  val regDataFull = regDataSimple ++ Map(
    "client_name" -> "A name",
    "client_brand" -> "A brand",
    "client_model" -> "A model",
    "client_os" -> "An OS"
  )

  val userIdExpr = """urn:blinkbox:zuul:user:(\d+)""".r
  val userUriExpr = """\/users\/(\d+)""".r
  val clientIdExpr = """urn:blinkbox:zuul:client:(\d+)""".r
  val clientUriExpr = """\/clients\/(\d+)""".r

  "The service" should "allow the registration of a new user without a client" in {
    Post("/oauth2/token", FormData(regDataSimple)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      responseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", 1800, Some(_), userIdExpr(_), userUriExpr(_), "user@domain.test", "First", "Last", None, None, None, None, None, None, None, None) =>
      }
    }
  }

  it should "allow the registration of a new user with a client" in {
    Post("/oauth2/token", FormData(regDataFull)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      responseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", 1800, Some(_), userIdExpr(_), userUriExpr(_), "user@domain.test", "First", "Last",
          Some(clientIdExpr(_)), Some(clientUriExpr(_)), Some("A name"), Some("A brand"), Some("A model"), Some("An OS"), Some(_), Some(_)) =>
      }
    }
  }

  it should "not allow the registration of a new user with partial client data" in {
    Post("/oauth2/token", FormData(regDataFull - "client_model")) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      responseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidRequest, None) =>
      }
    }
  }

  it should "not allow the registration of a new user with an existing username" in {
    Post("/oauth2/token", FormData(regDataSimple.updated("username", "user.a@test.tst"))) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      responseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidRequest, Some(ZuulRequestErrorReason.UsernameAlreadyTaken)) =>
      }
    }
  }
}
