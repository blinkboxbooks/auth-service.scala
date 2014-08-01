package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.{Serialization, TestEnv}
import org.scalatest.{BeforeAndAfterEach, Matchers, FlatSpec}
import spray.http.MediaTypes
import spray.httpx.Json4sJacksonSupport
import spray.httpx.unmarshalling._
import spray.routing._
import spray.testkit.ScalatestRouteTest

import scala.reflect.ClassTag

abstract class SpecBase extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with HttpService
  with BeforeAndAfterEach
  with FormDataUnmarshallers {

  def actorRefFactory = system

  private def newEnv = new TestEnv {}

  var env: TestEnv = _
  var route: Route = _

  val userIdExpr = """urn:blinkbox:zuul:user:(\d+)""".r
  val userUriExpr = """\/users\/(\d+)""".r
  val clientIdExpr = """urn:blinkbox:zuul:client:(\d+)""".r
  val clientUriExpr = """\/clients\/(\d+)""".r

  override def beforeEach() {
    env = newEnv
    route = env.zuulRoutes
  }

  def jsonResponseAs[T: FromResponseUnmarshaller: ClassTag]: T = {
    mediaType should equal(MediaTypes.`application/json`)
    responseAs[T]
  }
}
