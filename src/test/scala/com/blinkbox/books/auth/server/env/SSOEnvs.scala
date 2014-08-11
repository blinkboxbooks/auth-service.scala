package com.blinkbox.books.auth.server.env

import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}

trait RegistrationResponder {
  this: TestSSOComponent =>

  val registrationJson = """{
    "access_token":"eyJhbGciOiJFUzI1NiJ9.eyJzY3AiOlsic3NvOmJvb2tzIl0sImV4cCI6MTQwNjU1NjU5OSwic3ViIjoiQjBFODQyOEUtN0RFQi00MEJGLUJGQkUtNUQwOTI3QTU0RjY1IiwicmlkIjoiNEY3N0M1RkEtNTJCQy00RDY0LUI0OUItOTMyNUY3ODE1NEYwIiwibG5rIjpbXSwic3J2IjoiYm9va3MiLCJyb2wiOltdLCJ0a3QiOiJhY2Nlc3MiLCJpYXQiOjE0MDY1NTQ3OTl9.lTtM96tL9ALtZPd8Ct28dt4BinWuru6L-nXqMANro14N0SKcOJhJppfEOC2y8CUEQ_XN55WA2IdTm1ebIUV9gQ",
    "token_type":"bearer",
    "expires_in":600,
    "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
  }"""

  def ssoSuccessfulRegistration(): Unit = ssoResponse.complete(
      _.success(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, registrationJson.getBytes))),
      _.success(HttpResponse(StatusCodes.NoContent))
    )
}

class RegistrationTestEnv extends TestEnv with RegistrationResponder {
  ssoSuccessfulRegistration()
}
