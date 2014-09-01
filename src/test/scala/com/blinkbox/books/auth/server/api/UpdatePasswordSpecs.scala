package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.env.{CommonResponder, TestEnv}
import com.blinkbox.books.auth.server.sso.{SsoAccessToken, SsoCredentials, SsoTestEnv}
import com.blinkbox.books.auth.server.{TokenBuilder, UserInfo}
import spray.http.{HttpEntity, FormData, OAuth2BearerToken, StatusCodes}

class UpdatePasswordSpecs extends ApiSpecBase[TestEnv with CommonResponder] {

  override def newEnv = new TestEnv with CommonResponder

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
    env.ssoNoInvocation()

    Post("/password/change", passwordRequest) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Unauthorized)
    }
  }
}
