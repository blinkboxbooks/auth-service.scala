package com.blinkbox.books.auth.server.env

import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import spray.http.HttpHeaders.RawHeader
import spray.http._

trait SSOResponseFixtures {
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

  val userInfoJson = s"""{
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

  def sessionInfoJson(status: SSOTokenStatus, elevation: SSOTokenElevation) = s"""{
    "status": "${SSOTokenStatus.toString(status)}",
    "issued_at": "2000-01-01T01:01:01.01Z",
    "expires_at": "2020-01-01T01:01:01.01Z",
    "token_type": "refresh",
    "session_elevation": "${SSOTokenElevation.toString(elevation)}",
    "session_elevation_expires_in": 300
  }"""
}

trait CommonResponder extends SSOResponseFixtures {
  this: TestSSOComponent =>

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
}

trait RegistrationResponder extends CommonResponder {
  this: TestSSOComponent =>

  def ssoSuccessfulRegistration(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes))),
      _.success(HttpResponse(StatusCodes.NoContent))
    )
}

trait AuthenticationResponder extends CommonResponder {
  this: TestSSOComponent =>

  def ssoSuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes)))
    )
  
  def ssoUnsuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.Unauthorized, HttpEntity.Empty))
    )

  def ssoTooManyRequests(retryAfter: Int): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.TooManyRequests, HttpEntity.Empty, RawHeader("Retry-After", retryAfter.toString) :: Nil))
    )
}

trait UserInfoResponder extends CommonResponder {
  this: TestSSOComponent =>

  def ssoSuccessfulUserInfo(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, userInfoJson.getBytes)))
    )
}

trait TokenStatusResponder extends CommonResponder {
  this: TestSSOComponent =>

  def ssoSessionInfo(status: SSOTokenStatus, elevation: SSOTokenElevation): Unit = ssoResponse.complete(
    _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, sessionInfoJson(status, elevation).getBytes)))
  )
}

class RegistrationTestEnv extends TestEnv with RegistrationResponder

class AuthenticationTestEnv extends TestEnv with AuthenticationResponder

class LinkTestEnv extends TestEnv with CommonResponder

class RefreshTestEnv extends TestEnv with AuthenticationResponder

class UserInfoTestEnv extends TestEnv with UserInfoResponder

class TokenStatusEnv extends TestEnv with TokenStatusResponder
