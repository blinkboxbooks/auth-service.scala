package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data.UserId

// TODO: IP-related scenarios and scenarios with failures from SSO are not being tested at the moment, add those tests
class DefaultRegistrationServiceSpecs extends SpecBase {

  import env._

  val simpleReg = UserRegistration("New First", "New Last", "new.user@test.tst", "new-password", true, true, None, None, None, None)
  val clientReg = UserRegistration("New First", "New Last", "new.user@test.tst", "new-password", true, true,
    Some("New Name"), Some("New Brand"), Some("New Model"), Some("New OS"))

  def assertUserRegistered(token: TokenInfo, regId: Int): Unit = {
    token.user_first_name should equal("New First")
    token.user_last_name should equal("New Last")
    token.user_username should equal("new.user@test.tst")
    token.user_id should equal(UserId(regId).external)
    token.user_uri should equal(s"/users/$regId")
  }

  "The registration service" should "register a user without a client and no IP" in {
    ssoSuccessfulRegistration()

    import driver.simple._

    whenReady(registrationService.registerUser(simpleReg, None)) { token =>

      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }

      assertUserRegistered(token, regId)
      token.refresh_token shouldBe defined
      token.client_name shouldBe empty
      token.client_brand shouldBe empty
      token.client_model shouldBe empty
      token.client_os shouldBe empty
    }
  }

  it should "register a user with a client" in {
    ssoSuccessfulRegistration()

    whenReady(registrationService.registerUser(clientReg, None)) { token =>

      import driver.simple._

      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }

      assertUserRegistered(token, regId)
      token.refresh_token shouldBe defined
      token.client_name shouldBe Some("New Name")
      token.client_brand shouldBe Some("New Brand")
      token.client_model shouldBe Some("New Model")
      token.client_os shouldBe Some("New OS")
    }
  }

  it should "correctly signal when an username is already registered with the SSO service" in {
    ssoConflict()

    failingWith[ZuulRequestException](registrationService.registerUser(clientReg, None)) should equal(Failures.usernameAlreadyTaken)
  }

  it should "correctly signal when the SSO service returns validation errors" in {
    val err = "Validation errors"
    ssoInvalidRequest(err)

    failingWith[ZuulRequestException](registrationService.registerUser(clientReg, None)) should equal(
      Failures.requestException(err, ZuulRequestErrorCode.InvalidRequest))
  }
}
