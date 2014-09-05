package com.blinkbox.books.auth.server.api

import spray.http.StatusCodes

class HealthCheckSpecs extends ApiSpecBase {

  "The service" should "respond to ping requests" in {
    Get("/health/ping") ~> route ~> check {
      status should equal(StatusCodes.OK)
    }
  }

  it should "respond to health report requests" in {
    Get("/health/report") ~> route ~> check {
      status should equal(StatusCodes.OK)
    }
  }

  it should "respond to thread dump requests" in {
    Get("/health/threads") ~> route ~> check {
      status should equal(StatusCodes.OK)
    }
  }

}
