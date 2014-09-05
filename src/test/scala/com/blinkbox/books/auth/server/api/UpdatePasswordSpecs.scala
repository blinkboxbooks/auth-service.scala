package com.blinkbox.books.auth.server.api

import spray.http.{FormData, HttpEntity, OAuth2BearerToken, StatusCodes}

class UpdatePasswordSpecs extends ApiSpecBase {

  val passwordRequest = FormData(Map(
    "old_password" -> "Foo",
    "new_password" -> "Bar"
  ))

  "The service" should "report a success if the SSO service accepts a password change request" in {
    env.ssoNoContent()

    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)
      response.entity should equal(HttpEntity.Empty)
    }
  }

  it should "report a BadRequest if the SSO service doesn't recognize the old password" in {
    env.ssoResponse(StatusCodes.Forbidden)

    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "report a BadRequest if the SSO service doesn't accept the new password because it doesn't meet requirements" in {
    env.ssoInvalidRequest("Password does not meet minimum requirements")

    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.BadRequest)
    }
  }

  it should "report a TooManyRequest if the SSO service respond with a too-many-request response" in {
    env.ssoTooManyRequests(20)

    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.TooManyRequests)
      header("Retry-After").map(_.value) should equal(Some("20"))
    }
  }

  it should "report an Unauthorized if the user does not have an SSO access token" in {
    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
    }
  }
}
