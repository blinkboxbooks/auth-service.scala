package com.blinkbox.books.auth.server.env

import com.blinkbox.books.auth.server.sso.{SsoTokenElevation, SsoTokenStatus}
import spray.http.HttpHeaders.RawHeader
import spray.http._

trait SsoResponseFixtures {
  val validTokenSSOExpiry = 600
  val validTokenZuulExpiry = 540 // 1 minute before SSO token
  val validTokenJson = s"""{
    "access_token":"eyJhbGciOiJFUzI1NiJ9.eyJzY3AiOlsic3NvOmJvb2tzIl0sImV4cCI6MTQwNjU1NjU5OSwic3ViIjoiQjBFODQyOEUtN0RFQi00MEJGLUJGQkUtNUQwOTI3QTU0RjY1IiwicmlkIjoiNEY3N0M1RkEtNTJCQy00RDY0LUI0OUItOTMyNUY3ODE1NEYwIiwibG5rIjpbXSwic3J2IjoiYm9va3MiLCJyb2wiOltdLCJ0a3QiOiJhY2Nlc3MiLCJpYXQiOjE0MDY1NTQ3OTl9.lTtM96tL9ALtZPd8Ct28dt4BinWuru6L-nXqMANro14N0SKcOJhJppfEOC2y8CUEQ_XN55WA2IdTm1ebIUV9gQ",
    "token_type":"bearer",
    "expires_in":$validTokenSSOExpiry,
    "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
  }"""

  def invalidRequestJson(description: String, error: String) = s"""{
    "error": "$error",
    "error_description": "$description"
  }"""

  val johnDoeInfoJson = s"""{
    "user_id":"6E41CB9F",
    "username":"john.doe+blinkbox@example.com",
    "email":"john.doe@example.com",
    "first_name":"John",
    "last_name":"Doe",
    "date_of_birth": "1985-04-12",
    "gender": "M",
    "group_allow_marketing": false,
    "validated": true,
    "linked_accounts": [
      {
        "service":"music",
        "service_user_id":"john.doe@music.com",
        "service_linked_on": "2012-04-12T23:20:50.52Z",
        "service_allow_marketing": true,
        "service_tc_accepted_version": "v2.0"
      }
    ]
  }"""

  val userAInfoJson = s"""{
    "user_id":"6E41CB9F",
    "username":"user.a@test.tst",
    "email":"user.a@test.tst",
    "first_name":"A First",
    "last_name":"A Last",
    "date_of_birth": "1985-04-12",
    "gender": "M",
    "group_allow_marketing": false,
    "validated": true,
    "linked_accounts": [
      {
        "service":"music",
        "service_user_id":"john.doe@music.com",
        "service_linked_on": "2012-04-12T23:20:50.52Z",
        "service_allow_marketing": true,
        "service_tc_accepted_version": "v2.0"
      }
    ]
  }"""

  def sessionInfoJson(status: SsoTokenStatus, elevation: SsoTokenElevation, tokenType: String) = s"""{
    "status": "${SsoTokenStatus.toString(status)}",
    "issued_at": "2000-01-01T01:01:01.01Z",
    "expires_at": "2020-01-01T01:01:01.01Z",
    "token_type": "$tokenType",
    "session_elevation": "${SsoTokenElevation.toString(elevation)}",
    "session_elevation_expires_in": 300
  }"""

  val resetTokenJson = s"""{
    "reset_token": "r3sett0ken",
    "expires_in": 3600
  }""".stripMargin
}

trait CommonResponder extends SsoResponseFixtures {
  this: TestSsoComponent =>

  def ssoNoInvocation() = ssoResponse.complete(_.failure(new IllegalStateException("No invocation for SSO was expected")))

  def ssoInvalidRequest(description: String, error: String = "invalid_request"): Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.BadRequest,
      HttpEntity(ContentTypes.`application/json`, invalidRequestJson(description, error).getBytes)))
  )

  def ssoResponse(statusCode : StatusCode, entity: HttpEntity = HttpEntity.Empty): Unit = ssoResponse.complete(_.success(HttpResponse(statusCode, entity)))

  def ssoConflict(): Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.Conflict, HttpEntity.Empty))
  )

  def ssoNoContent(): Unit = ssoResponse.complete(_.success(HttpResponse(StatusCodes.NoContent, HttpEntity.Empty)))

  def ssoTooManyRequests(retryAfter: Int): Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.TooManyRequests, HttpEntity.Empty, RawHeader("Retry-After", retryAfter.toString) :: Nil))
  )
}

trait RegistrationResponder extends CommonResponder {
  this: TestSsoComponent =>

  def ssoSuccessfulRegistration(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes))),
      _.success(HttpResponse(StatusCodes.NoContent))
    )
}

trait AuthenticationResponder extends CommonResponder {
  this: TestSsoComponent =>

  def ssoSuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes)))
    )
  
  def ssoUnsuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.Unauthorized, HttpEntity.Empty))
    )
}

trait UserInfoResponder extends CommonResponder {
  this: TestSsoComponent =>

  def ssoSuccessfulJohnDoeInfo(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, johnDoeInfoJson.getBytes)))
    )

  def ssoSuccessfulUserAInfo(): Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, userAInfoJson.getBytes)))
  )
}

trait TokenStatusResponder extends CommonResponder {
  this: TestSsoComponent =>

  def ssoSessionInfo(status: SsoTokenStatus, elevation: SsoTokenElevation, tokenType: String = "refresh"): Unit =
    ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, sessionInfoJson(status, elevation, tokenType).getBytes)))
    )
}

trait PasswordResetResponder extends CommonResponder {
  this: TestSsoComponent =>

  def ssoGenerateResetToken: Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, resetTokenJson.getBytes)))
  )

  def ssoUserNotFound: Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.NotFound, HttpEntity.Empty))
  )
}

class RegistrationTestEnv extends TestEnv with RegistrationResponder

class AuthenticationTestEnv extends TestEnv with AuthenticationResponder with UserInfoResponder

class LinkTestEnv extends TestEnv with CommonResponder

class RefreshTestEnv extends TestEnv with AuthenticationResponder

class UserInfoTestEnv extends TestEnv with UserInfoResponder

class TokenStatusEnv extends TestEnv with TokenStatusResponder

class PasswordResetEnv extends TestEnv with PasswordResetResponder with AuthenticationResponder with UserInfoResponder with TokenStatusResponder
