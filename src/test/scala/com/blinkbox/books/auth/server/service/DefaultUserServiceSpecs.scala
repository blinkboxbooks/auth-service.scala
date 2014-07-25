package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.data.{User, UserId}
import com.blinkbox.books.auth.server.events.UserUpdated
import com.blinkbox.books.testkit.TestH2
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

class DefaultUserServiceSpecs extends FlatSpec with Matchers with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(500, Millis), interval = Span(20, Millis))

  import com.blinkbox.books.testkit.TestH2.tables._
  import driver.simple._

  "The user service" should "retrieve the dummy user" in new DefaultH2TestEnv {
    whenReady(userService.getUserInfo(userIdA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("A First")
        info.user_last_name should equal("A Last")
        info.user_username should equal("user.a@test.tst")
        info.user_id should equal("urn:blinkbox:zuul:user:1")
        info.user_allow_marketing_communications should equal(true)
      }
    }
  }

  it should "return a None when looking up non-existing ids" in new DefaultH2TestEnv {
    whenReady(userService.getUserInfo(UserId(100))) { infoOpt =>
      infoOpt shouldBe empty
    }
  }

  it should "update an user given new details" in new DefaultH2TestEnv {
    whenReady(userService.updateUser(UserId(1), fullUserPatch)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("Updated First")
        info.user_last_name should equal("Updated Last")
        info.user_username should equal("updated@test.tst")
        info.user_id should equal("urn:blinkbox:zuul:user:1")
        info.user_allow_marketing_communications should equal(false)
      }

      val updated = db.withSession { implicit session =>
        tables.users.where(_.id === UserId(1)).firstOption
      }

      val expectedUpdatedUser = User(UserId(1), cl.now, cl.now, "updated@test.tst", "Updated First", "Updated Last", "a-password", false)

      updated shouldBe defined
      updated foreach { _ shouldEqual expectedUpdatedUser}
      
      publisherSpy.events shouldEqual(UserUpdated(userA, expectedUpdatedUser) :: Nil)
    }
  }
}
