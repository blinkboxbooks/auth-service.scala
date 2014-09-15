package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import spray.http._

class RegistrationSpecs extends ApiSpecBase {

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

  "The service" should "allow the registration of a new user without a client" in {
    env.ssoSuccessfulRegistrationAndLink()

    Post("/oauth2/token", FormData(regDataSimple)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, Some(_), ExternalUserId(_), userUriExpr(_), "user@domain.test", "First", "Last", None, None, None, None, None, None, None, None) =>
      }
    }
  }

  it should "allow the registration of a new user without a client and preserve non-ASCII characters" in {
    env.ssoSuccessfulRegistrationAndLink()

    val firstName = "Iñtërnâtiônàlizætiøn"
    val lastName = "中国扬声器可以阅读本"

    val regDataNonAscii = regDataSimple.updated("first_name", firstName).updated("last_name", lastName)

    Post("/oauth2/token", FormData(regDataNonAscii)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      val resp = jsonResponseAs[TokenInfo]

      import env.driver.simple._
      val regUser = env.db.withSession { implicit session =>
        env.tables.users.sortBy(_.id).list.last
      }

      regUser.firstName should equal(firstName)
      regUser.lastName should equal(lastName)

      resp.user_first_name should equal(firstName)
      resp.user_last_name should equal(lastName)
    }
  }

  it should "allow the registration of a new user with a client" in {
    env.ssoSuccessfulRegistrationAndLink()

    Post("/oauth2/token", FormData(regDataFull)) ~> route ~> check {
      status should equal(StatusCodes.OK)

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, Some(_), ExternalUserId(_), userUriExpr(_), "user@domain.test", "First", "Last",
          Some(ExternalClientId(_)), Some(clientUriExpr(_)), Some("A name"), Some("A brand"), Some("A model"), Some("An OS"), Some(_), Some(_)) =>
      }
    }
  }

  it should "not allow the registration of a new user with partial client data" in {
    Post("/oauth2/token", FormData(regDataFull - "client_model")) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidRequest, None) =>
      }
    }
  }

  it should "not allow the registration of a new user with an existing username" in {
    env.ssoConflict()

    Post("/oauth2/token", FormData(regDataSimple.updated("username", "user.a@test.tst"))) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidRequest, Some(ZuulRequestErrorReason.UsernameAlreadyTaken)) =>
      }
    }
  }
}
