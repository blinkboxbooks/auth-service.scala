package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.server.data.User

trait UserInfoFactory {
  protected def userInfoFromUser(user: User) = UserInfo(
    user_id = s"urn:blinkbox:zuul:user:${user.id}",
    user_uri = s"/users/${user.id}",
    user_username = user.username,
    user_first_name = user.firstName,
    user_last_name = user.lastName,
    user_allow_marketing_communications = user.allowMarketing
  )
}
