package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.env.AuthenticationTestEnv
import spray.http.{FormData, StatusCodes}

class AuthenticationSpecs extends ApiSpecBase[AuthenticationTestEnv] {

  lazy val validCredentials = Map(
    "grant_type" -> "password",
    "username" -> env.userA.username,
    "password" -> env.userA.passwordHash
  )

  lazy val clientCredentials = Map(
    "client_id" -> env.clientInfoA1.client_id,
    "client_secret" -> env.clientA1.secret
  )

  lazy val deregisteredClientCredentials = Map(
    "client_id" -> ClientId(3).external,
    "client_secret" -> env.clientA3.secret
  )

  lazy val validCredentialsWithClient = validCredentials ++ clientCredentials
  lazy val validCredentialsWithDeregisteredClient = validCredentials ++ deregisteredClientCredentials

  lazy val validRefreshTokenCredentials = Map(
    "grant_type" -> "refresh_token",
    "refresh_token" -> env.refreshTokenNoClientA.token
  )

  lazy val revokedRefreshTokenCredentials = Map(
    "grant_type" -> "refresh_token",
    "refresh_token" -> env.refreshTokenNoClientDeregisteredA.token
  )

  lazy val validRefreshTokenCredentialsWithClient = Map(
    "grant_type" -> "refresh_token",
    "refresh_token" -> env.refreshTokenClientA1.token
  ) ++ clientCredentials

  lazy val validRefreshTokenCredentialsWithDeregisteredClient = Map(
    "grant_type" -> "refresh_token",
    "refresh_token" -> env.refreshTokenClientA3.token
  ) ++ deregisteredClientCredentials

  override def newEnv = new AuthenticationTestEnv

  "The service" should "accept valid username/password pair returning a valid access token" in {
    env.ssoSuccessfulAuthentication()
    Post("/oauth2/token", FormData(validCredentials)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      val u = env.userA

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, Some(_), ExternalUserId(_), userUriExpr(_), u.username, u.firstName, u.lastName,
          None, None, None, None, None, None, None, None) =>
      }
    }
  }

  it should "accept valid username/password pair and client credentials returning a valid access token" in {
    env.ssoSuccessfulAuthentication()
    Post("/oauth2/token", FormData(validCredentialsWithClient)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      val u = env.userA
      val c = env.clientInfoA1

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, Some(_), ExternalUserId(_), userUriExpr(_), "user.a@test.tst", "A First", "A Last",
          Some(c.client_id), Some(c.client_uri), Some(c.client_name), Some(c.client_brand), Some(c.client_model), Some(c.client_os), None, Some(_)) =>
      }
    }
  }

  it should "reject incomplete client information" in {
    env.ssoUnsuccessfulAuthentication()
    Post("/oauth2/token", FormData(validCredentialsWithClient - "client_secret")) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidClient, None) =>
      }
    }
  }

  it should "reject invalid username/password credentials" in {
    env.ssoUnsuccessfulAuthentication()
    Post("/oauth2/token", FormData(validCredentialsWithClient.updated("password", "invalid"))) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }

  it should "reject credentials for a de-registerd client" in {
    env.ssoSuccessfulAuthentication()
    Post("/oauth2/token", FormData(validCredentialsWithDeregisteredClient)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidClient, None) =>
      }
    }
  }

  it should "accept a valid refresh token" in {
    env.ssoSuccessfulAuthentication()

    Post("/oauth2/token", FormData(validRefreshTokenCredentials)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      val u = env.userA

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, None, ExternalUserId(_), userUriExpr(_), u.username, u.firstName, u.lastName,
          None, None, None, None, None, None, None, None) =>
      }
    }
  }

  it should "accept a valid refresh token with valid client information" in {
    env.ssoSuccessfulAuthentication()

    Post("/oauth2/token", FormData(validRefreshTokenCredentialsWithClient)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.OK)

      val u = env.userA
      val c = env.clientInfoA1

      jsonResponseAs[TokenInfo] should matchPattern {
        case TokenInfo(_, "bearer", _, None, ExternalUserId(_), userUriExpr(_), "user.a@test.tst", "A First", "A Last",
          Some(c.client_id), Some(c.client_uri), Some(c.client_name), Some(c.client_brand), Some(c.client_model), Some(c.client_os), None, Some(_)) =>
      }
    }
  }

  it should "reject an invalid refresh token" in {
    env.ssoNoInvocation()

    Post("/oauth2/token", FormData(validRefreshTokenCredentials.updated("refresh_token", "invalid"))) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }

  it should "reject a valid refresh token with invalid client information" in {
    env.ssoNoInvocation()

    Post("/oauth2/token", FormData(validRefreshTokenCredentialsWithClient.updated("client_id", "invalid"))) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidClient, None) =>
      }
    }
  }

  it should "reject a valid refresh token with de-registered client information" in {
    env.ssoNoInvocation()

    Post("/oauth2/token", FormData(validRefreshTokenCredentialsWithDeregisteredClient)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }

  it should "reject a revoked refresh token" in {
    env.ssoNoInvocation()

    Post("/oauth2/token", FormData(revokedRefreshTokenCredentials)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }

  it should "reject a refresh token if the associated client credentials are not provided" in {
    env.ssoNoInvocation()

    Post("/oauth2/token", FormData(validRefreshTokenCredentialsWithClient - "client_id" - "client_secret")) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidClient, None) =>
      }
    }
  }

  it should "revoke a valid refresh token and not allow its usage any more" in {
    Post("/tokens/revoke", FormData(validRefreshTokenCredentials - "grant_type")) ~> route ~> check {
      status should equal(StatusCodes.OK)
    }

    Post("/oauth2/token", FormData(validRefreshTokenCredentials)) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }

  it should "respond with an error when trying to revoke an invalid refresh token" in {
    Post("/tokens/revoke", FormData(Map("refresh_token" -> "invalid"))) ~> route ~> check {
      import com.blinkbox.books.auth.server.Serialization._

      status should equal(StatusCodes.BadRequest)

      jsonResponseAs[ZuulRequestException] should matchPattern {
        case ZuulRequestException(_, ZuulRequestErrorCode.InvalidGrant, None) =>
      }
    }
  }
}
