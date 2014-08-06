package com.blinkbox.books.auth.server.env

import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}

trait RegistrationResponder {
  this: TestSSOComponent =>

  val registrationJson = """{
    "access_token":"2YotnFZFEjr1zCsicMWpAA",
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
