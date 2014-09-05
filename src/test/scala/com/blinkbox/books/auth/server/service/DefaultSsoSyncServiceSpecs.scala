package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.data.{User, UserId}
import com.blinkbox.books.auth.server.events.{UserRegistered, UserUpdated}
import com.blinkbox.books.auth.server.sso.{SsoAccessToken, SsoUserId}

class DefaultSsoSyncServiceSpecs extends SpecBase {

  import env._

  "The Sso sync service" should "create an user if a None is provide" in {
    ssoSuccessfulJohnDoeInfo()
    ssoNoContent()

    whenReady(ssoSync(None, SsoAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(UserId(_), _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, false, Some(SsoUserId("6E41CB9F"))) =>
      }

      import driver.simple._
      import tables._

      db.withSession { implicit session =>
        val dbUser = users.filter(_.id === user.id).firstOption
        dbUser should equal(Some(user))
      }

      publisher.events should equal(UserRegistered(user) :: Nil)
    }
  }

  it should "link and update an user if one not having SSO id has been provided" in {
    ssoNoContent()
    ssoSuccessfulJohnDoeInfo()

    whenReady(ssoSync(Some(userC), SsoAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(id, _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, true, Some(SsoUserId("6E41CB9F"))) if id == userIdC =>
      }

      import driver.simple._
      import tables._

      db.withSession { implicit session =>
        val dbUser = users.filter(_.id === userIdC).firstOption
        dbUser should equal(Some(user))
      }

      publisher.events should equal(UserUpdated(userC, user) :: Nil)
    }
  }

  it should "update an user if one having SSO id has been provided" in {
    ssoSuccessfulJohnDoeInfo()

    whenReady(ssoSync(Some(userA), SsoAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(id, _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, true, Some(SsoUserId("6E41CB9F"))) if id == userIdA =>
      }

      import driver.simple._
      import tables._

      db.withSession { implicit session =>
        val dbUser = users.filter(_.id === userIdA).firstOption
        dbUser should equal(Some(user))
      }

      publisher.events should equal(UserUpdated(userA, user) :: Nil)
    }
  }
}
