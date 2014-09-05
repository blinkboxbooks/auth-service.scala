package com.blinkbox.books.auth.server.sso

import spray.http.{HttpEntity, StatusCodes}

class UpdateUserSpecs extends SpecBase {

  import env._

  "The SSO client" should "update user information if provided a patch" in {
    ssoNoContent()

    whenReady(sso.updateUser(SsoAccessToken("some-access-token"), fullUserPatch)) { _ =>}
  }

  it should "signal invalid credentials in case of an unauthorized response from the service" in {
    ssoResponse(StatusCodes.Unauthorized, HttpEntity.Empty)

    failingWith[SsoUnauthorized.type](sso.updateUser(SsoAccessToken("some-access-token"), fullUserPatch))
  }
}
