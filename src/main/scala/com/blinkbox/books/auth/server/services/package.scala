package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.server.data.RefreshTokenId
import shapeless.Typeable._

package object services {

  implicit class UserOps(user: User) {
    def refreshTokenId: Option[RefreshTokenId] = user.
      claims.
      get("zl/rti").
      flatMap(_.cast[Int]).
      map(RefreshTokenId.apply)
  }
}
