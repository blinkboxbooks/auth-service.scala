package com.blinkbox.books.auth.server.env

import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}

trait SSOResponseFixtures {
  val validTokenSSOExpiry = 600
  val validTokenZuulExpiry = 540 // 1 minute before SSO token
  val validTokenJson = s"""{
    "access_token":"eyJhbGciOiJFUzI1NiJ9.eyJzY3AiOlsic3NvOmJvb2tzIl0sImV4cCI6MTQwNjU1NjU5OSwic3ViIjoiQjBFODQyOEUtN0RFQi00MEJGLUJGQkUtNUQwOTI3QTU0RjY1IiwicmlkIjoiNEY3N0M1RkEtNTJCQy00RDY0LUI0OUItOTMyNUY3ODE1NEYwIiwibG5rIjpbXSwic3J2IjoiYm9va3MiLCJyb2wiOltdLCJ0a3QiOiJhY2Nlc3MiLCJpYXQiOjE0MDY1NTQ3OTl9.lTtM96tL9ALtZPd8Ct28dt4BinWuru6L-nXqMANro14N0SKcOJhJppfEOC2y8CUEQ_XN55WA2IdTm1ebIUV9gQ",
    "token_type":"bearer",
    "expires_in":$validTokenSSOExpiry,
    "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
  }"""
}

trait CommonResponder {
  this: TestSSOComponent =>

  def ssoNoInvocation() = ssoResponse.complete(_.failure(new IllegalStateException("No invocation for SSO was expected")))
}

trait RegistrationResponder extends CommonResponder with SSOResponseFixtures {
  this: TestSSOComponent =>

  def ssoRegistrationConflict(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.Conflict, HttpEntity.Empty))
    )

  def ssoSuccessfulRegistration(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes))),
      _.success(HttpResponse(StatusCodes.NoContent))
    )
}

trait AuthenticationResponder extends CommonResponder with SSOResponseFixtures {
  this: TestSSOComponent =>

  def ssoSuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, validTokenJson.getBytes)))
    )
  
  def ssoUnsuccessfulAuthentication(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.Unauthorized, HttpEntity.Empty))
    )
}

class RegistrationTestEnv extends TestEnv with RegistrationResponder

class AuthenticationTestEnv extends TestEnv with AuthenticationResponder
