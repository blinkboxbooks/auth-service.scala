package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.{ClientInfo, UserInfo}
import com.blinkbox.books.auth.server.data.{Client, User}

trait UserInfoFactory {
  def userInfoFromUser(user: User) = UserInfo(
    user_id = s"urn:blinkbox:zuul:user:${user.id.value}",
    user_uri = s"/users/${user.id.value}",
    user_username = user.username,
    user_first_name = user.firstName,
    user_last_name = user.lastName,
    user_allow_marketing_communications = user.allowMarketing
  )
}

trait ClientInfoFactory {
  def clientInfo(client: Client, includeClientSecret: Boolean = false) = ClientInfo(
    client_id = s"urn:blinkbox:zuul:client:${client.id.value}",
    client_uri = s"/clients/${client.id.value}",
    client_name = client.name,
    client_brand = client.brand,
    client_model = client.model,
    client_os = client.os,
    client_secret = if (includeClientSecret) Some(client.secret) else None,
    last_used_date = client.createdAt)
}
