package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.ClientInfo
import com.blinkbox.books.auth.server.env.TestEnv
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.{OAuth2BearerToken, StatusCodes}

class ClientInfoSpecs extends ApiSpecBase[TestEnv] {

  override def newEnv = new TestEnv

  "The service" should "return client info for an SSO user's client" in {
    Get("/clients/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)
      jsonResponseAs[ClientInfo] should equal(env.clientInfoA1)
    }
  }

  it should "return client info for a non-SSO user's client" in {
    Get("/clients/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)
      jsonResponseAs[ClientInfo] should equal(env.clientInfoA1)
    }
  }

  it should "return an uncacheable response" in {
    Get("/clients/1") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      header[`Cache-Control`] should equal(Some(`Cache-Control`(`no-store` :: Nil)))
      header("Pragma") should equal(Some(RawHeader("Pragma", "no-cache")))
    }
  }

  it should "return a 401 with no error code if no access token is supplied" in {
    shouldBeUnauthorisedWithMissingAccessToken(Get("/clients/1"), route)
  }

  it should "return a 401 with an invalid token error code if an invalid access token is supplied" in {
    shouldBeUnauthorisedWithInvalidAccessToken(Get("/clients/1"), route)
  }

  it should "return a 404 if a non-existent client is requested" in {
    Get("/clients/999999") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

  it should "return a 404 if client belonging to another user is requested" in {
    Get("/clients/5") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

}
