package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.data.{User, UserId}
import com.blinkbox.books.auth.server.env.{CommonResponder, TestEnv, UserInfoResponder}
import com.blinkbox.books.auth.server.events.UserUpdated
import com.blinkbox.books.auth.server.{Failures, ZuulAuthorizationException, ZuulUnknownException}
import spray.http.{HttpEntity, StatusCodes}

class DefaultUserServiceSpecs extends SpecBase {

  import com.blinkbox.books.testkit.TestH2.tables._
  import driver.simple._

  "The user service" should "retrieve an user and update its data with data coming from SSO" in new TestEnv with UserInfoResponder {
    ssoSuccessfulUserInfo()

    whenReady(userService.getUserInfo()(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("John")
        info.user_last_name should equal("Doe")
        info.user_username should equal("john.doe+blinkbox@example.com")
        info.user_id should equal(userIdA.external)
        info.user_allow_marketing_communications should equal(true)
      }

      val updated = db.withSession { implicit session => tables.users.where(_.id === userIdA).firstOption }

      val expected = User(userIdA, now, now, "john.doe+blinkbox@example.com", "John", "Doe", "a-password", true, Some("6E41CB9F"))

      updated should equal(Some(expected))

      publisherSpy.events should equal(UserUpdated(userA, expected) :: Nil)
    }
  }

  it should "signal an unverified identity if an user doesn't exist on our local system but exists in SSO" in new TestEnv with UserInfoResponder {
    ssoSuccessfulUserInfo()

    db.withSession { implicit session =>
      tables.refreshTokens.filter(_.userId === userIdA).mutate(_.delete)
      tables.users.filter(_.id === userIdA).mutate(_.delete)
    }

    failingWith[ZuulAuthorizationException](userService.getUserInfo()(authenticatedUserA)) should equal(Failures.unverifiedIdentity)
  }

  it should "signal an unverified identity if the user doesn't have an SSO access token" in new TestEnv with CommonResponder {
    ssoNoInvocation()

    failingWith[ZuulAuthorizationException](userService.getUserInfo()(authenticatedUserB)) should equal(Failures.unverifiedIdentity)
  }

  it should "not return any information if the user doesn't exist on SSO" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized, HttpEntity.Empty)

    failingWith[ZuulUnknownException](userService.getUserInfo()(authenticatedUserA))
  }

  it should "update an user given new details" in new TestEnv {
    whenReady(userService.updateUser(UserId(1), fullUserPatch)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("Updated First")
        info.user_last_name should equal("Updated Last")
        info.user_username should equal("updated@test.tst")
        info.user_id should equal(userIdA.external)
        info.user_allow_marketing_communications should equal(false)
      }

      val updated = db.withSession { implicit session =>
        tables.users.where(_.id === UserId(1)).firstOption
      }

      val expectedUpdatedUser = User(UserId(1), now, now, "updated@test.tst", "Updated First", "Updated Last", "a-password", false, Some("sso-a"))

      updated should equal(Some(expectedUpdatedUser))

      publisherSpy.events shouldEqual(UserUpdated(userA, expectedUpdatedUser) :: Nil)
    }
  }
}
