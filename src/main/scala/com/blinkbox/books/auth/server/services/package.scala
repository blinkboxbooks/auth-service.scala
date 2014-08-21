package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.server.data.RefreshTokenId
import com.blinkbox.books.auth.server.sso.SSOAccessToken
import shapeless.Typeable._

package object services {

  implicit class UserOps(user: User) {
    def ssoAccessToken: Option[SSOAccessToken] = user.
      claims.
      get("sso/at").
      flatMap(_.cast[String]).
      map(SSOAccessToken.apply)

    def refreshTokenId: Option[RefreshTokenId] = user.
      claims.
      get("zl/rti").
      flatMap(_.cast[Int]).
      map(RefreshTokenId.apply)
  }
}
