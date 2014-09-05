package com.blinkbox.books.auth.server.api

import java.lang.reflect.InvocationTargetException

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.{TokenStatus, ZuulRequestExceptionSerializer, env}
import com.blinkbox.books.json.DefaultFormats
import com.typesafe.config.ConfigFactory
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.Serialization
import org.json4s.{Formats, MappingException}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._
import spray.testkit.ScalatestRouteTest

import scala.reflect.ClassTag

trait JsonUnmarshallers {
  private implicit val jsonFormats: Formats = DefaultFormats + ZuulRequestExceptionSerializer +
    new EnumNameSerializer(TokenStatus) + new EnumNameSerializer(Elevation)

  implicit def jsonUnmarshaller[T: Manifest] =
    Unmarshaller[T](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty =>
        try Serialization.read[T](x.asString(defaultCharset = HttpCharsets.`UTF-8`))
        catch {
          case MappingException("unknown error", ite: InvocationTargetException) => throw ite.getCause
        }
    }
}

trait AuthorisationTestHelpers {
  this: ScalatestRouteTest with Matchers =>

  def shouldBeUnauthorisedWithMissingAccessToken(request: HttpRequest, route: Route): Unit =
    request ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
      header[`WWW-Authenticate`] should equal(Some(`WWW-Authenticate`(HttpChallenge("Bearer", "", Map()))))
    }

  def shouldBeUnauthorisedWithInvalidAccessToken(request: HttpRequest, route: Route): Unit =
    request ~> addCredentials(OAuth2BearerToken("faketoken")) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
      header[`WWW-Authenticate`] should equal(Some(`WWW-Authenticate`(HttpChallenge("Bearer", "", Map("error" -> "invalid_token", "error_description" -> "The access token is invalid")))))
    }
}

abstract class ApiSpecBase extends FlatSpec
  with env.SpecBase
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterEach
  with FormDataUnmarshallers
  with JsonUnmarshallers
  with AuthorisationTestHelpers {

  var route: Route = env.routes

  val userUriExpr = """\/users\/(\d+)""".r
  val clientUriExpr = """\/clients\/(\d+)""".r

  // Do not load the application configuration for the test actor system as it will be loaded later on. This is not optimal
  // but it solves the problem of having placeholders in the configuration that are resolved during the environment initialization.
  override def testConfig = ConfigFactory.empty()

  def jsonResponseAs[T: FromResponseUnmarshaller: ClassTag]: T = {
    mediaType should equal(MediaTypes.`application/json`)
    responseAs[T]
  }
}
