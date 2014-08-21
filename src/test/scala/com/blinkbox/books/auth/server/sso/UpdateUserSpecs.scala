package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.UserInfoTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.{HttpEntity, StatusCodes}

class UpdateUserSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "update user information if provided a patch" in new UserInfoTestEnv {
    ssoNoContent()

    whenReady(sso.updateUser(SSOAccessToken("some-access-token"), fullUserPatch)) { _ =>}
  }

  it should "signal invalid credentials in case of an unauthorized response from the service" in new UserInfoTestEnv {
    ssoResponse(StatusCodes.Unauthorized, HttpEntity.Empty)

    failingWith[SSOUnauthorized.type](sso.updateUser(SSOAccessToken("some-access-token"), fullUserPatch))
  }
}
