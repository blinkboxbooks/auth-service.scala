package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.{ClientList, ClientInfo}
import com.blinkbox.books.auth.server.env.TestEnv
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{`WWW-Authenticate`, RawHeader, `Cache-Control`}
import spray.http.{HttpRequest, HttpChallenge, OAuth2BearerToken, StatusCodes}
import spray.routing.Route

class ClientListSpecs extends ApiSpecBase[TestEnv] {

  override def newEnv = new TestEnv

  "The service" should "return a client list for an SSO user's clients" in {
    Get("/clients") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)
      jsonResponseAs[ClientList] should equal(ClientList(List(env.clientInfoA1, env.clientInfoA2)))
    }
  }

  it should "return a client list for a non-SSO user's client" in {
    Get("/clients") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1WithoutSSO.access_token)) ~> route ~> check {
      status should equal(StatusCodes.OK)
      jsonResponseAs[ClientList] should equal(ClientList(List(env.clientInfoA1, env.clientInfoA2)))
    }
  }

  it should "return an uncacheable response" in {
    Get("/clients") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      header[`Cache-Control`] should equal(Some(`Cache-Control`(`no-store` :: Nil)))
      header("Pragma") should equal(Some(RawHeader("Pragma", "no-cache")))
    }
  }

  it should "return a 401 with no error code if no access token is supplied" in {
    testMissingAccessToken(Get("/clients"), route)
  }

  it should "return a 401 with an invalid token error code if an invalid access token is supplied" in {
    testInvalidAccessToken(Get("/clients"), route)
  }

}
