package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.data.{UserId, User}
import com.blinkbox.books.auth.server.env.{UserInfoResponder, TestEnv}
import com.blinkbox.books.auth.server.events.{UserUpdated, UserRegistered}
import com.blinkbox.books.auth.server.sso.{SSOUnknownException, SSOUserId, SSOAccessToken}

class DefaultSsoSyncServiceSpecs extends SpecBase {

  "The Sso sync service" should "create an user if a None is provide" in new TestEnv with UserInfoResponder {
    ssoSuccessfulJohnDoeInfo()
    ssoNoContent()

    whenReady(ssoSync(None, SSOAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(UserId(_), _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, false, Some(SSOUserId("6E41CB9F"))) =>
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

  it should "link and update an user if one not having SSO id has been provided" in new TestEnv with UserInfoResponder {
    ssoNoContent()
    ssoSuccessfulJohnDoeInfo()

    whenReady(ssoSync(Some(userC), SSOAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(id, _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, true, Some(SSOUserId("6E41CB9F"))) if id == userIdC =>
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

  it should "update an user if one having SSO id has been provided" in new TestEnv with UserInfoResponder {
    ssoSuccessfulJohnDoeInfo()

    whenReady(ssoSync(Some(userA), SSOAccessToken("some-access-token"))){ user =>
      user should matchPattern {
        case User(id, _, _, "john.doe+blinkbox@example.com", "John", "Doe", _, true, Some(SSOUserId("6E41CB9F"))) if id == userIdA =>
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
