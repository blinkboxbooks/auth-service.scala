package com.blinkbox.books.server

import com.blinkbox.books.auth.server.data.{DefaultUserRepository, User}
import com.blinkbox.books.auth.server.{UserPatch, UserRegistration, DefaultUserService, PasswordHasher}
import com.blinkbox.books.testkit.{H2, PublisherDummy, TestGeoIP}
import com.blinkbox.books.time.StoppedClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

import scala.slick.driver.H2Driver

class DefaultUserServiceSpecs extends FlatSpec with Matchers with ScalaFutures {

  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val cl = StoppedClock()
  implicit override val patienceConfig = PatienceConfig(timeout = Span(500, Millis), interval = Span(20, Millis))

  import H2Driver.simple._

  trait TestEnv {
    val db = H2.db
    val userRepository = new DefaultUserRepository(H2Driver, PasswordHasher(identity))
    val userService = new DefaultUserService(H2.db, userRepository, TestGeoIP.geoIpStub(), PublisherDummy)

    val dummyUser = User(1, cl.now(), cl.now(), "dummy@dummy.dm", "Dummy", "Dummy", "dummypwd", true)
    val dummyUserPatch = UserPatch(Some("Updated Dummy"), Some("Updated Dummy"), Some("dummy+updated@dummy.dm"), Some(false), None)

    db.withSession { implicit session =>
      userRepository.users += dummyUser
    }

    val foobarWithoutClient = UserRegistration(
      firstName = "Foo",
      lastName = "Bar",
      username = "foobar@baz.com",
      password = "pazzword",
      acceptedTerms = true,
      allowMarketing = true,
      clientName = None,
      clientBrand = None,
      clientModel = None,
      clientOS = None
    )
  }

  "The user service" should "retrieve the dummy user" in new TestEnv {
    whenReady(userService.getUserInfo(dummyUser.id)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("Dummy")
        info.user_last_name should equal("Dummy")
        info.user_username should equal("dummy@dummy.dm")
        info.user_id should equal("urn:blinkbox:zuul:user:1")
        info.user_allow_marketing_communications should equal(true)
      }
    }
  }

  it should "update an user given new details" in new TestEnv {
    whenReady(userService.updateUser(1, dummyUserPatch)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt foreach { info =>
        info.user_first_name should equal("Updated Dummy")
        info.user_last_name should equal("Updated Dummy")
        info.user_username should equal("dummy+updated@dummy.dm")
        info.user_id should equal("urn:blinkbox:zuul:user:1")
        info.user_allow_marketing_communications should equal(false)
      }

      val updated = db.withSession { implicit session =>
        H2.tables.users.where(_.id === 1).firstOption
      }

      updated shouldBe defined
      updated foreach { _ shouldEqual User(1, cl.now, cl.now, "dummy+updated@dummy.dm", "Updated Dummy", "Updated Dummy", "dummypwd", false)}
    }
  }
}
