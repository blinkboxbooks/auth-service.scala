package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data.UserId
import spray.http.RemoteAddress

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
    ssoSuccessfulRegistrationAndLink()

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
    ssoSuccessfulRegistrationAndLink()

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

  it should "correctly reject registrations from outside the UK" in {
     failingWith[ZuulRequestException](registrationService.registerUser(simpleReg, Some(RemoteAddress("8.8.8.8")))) should equal(Failures.notInTheUK)
  }

  it should "correctly reject registrations from global IPs that cannot be resolved to a country" in {
    // This test uses the 6to4 Relay Anycast IP class which is global but not geo-locable (192.88.99.0/24)
    failingWith[ZuulRequestException](registrationService.registerUser(simpleReg, Some(RemoteAddress("192.88.99.1")))) should equal(Failures.notInTheUK)
  }

  it should "correctly reject registrations where terms and conditions have not been accepted" in {
    failingWith[ZuulRequestException](registrationService.registerUser(simpleReg.copy(acceptedTerms = false), None)) should equal(Failures.termsAndConditionsNotAccepted)
  }

  it should "correctly accept registrations from a loopback address" in {
    ssoSuccessfulRegistrationAndLink()

    import driver.simple._

    val addr = Some(RemoteAddress("127.0.0.1"))

    whenReady(registrationService.registerUser(simpleReg, addr)) { token =>
      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }
      assertUserRegistered(token, regId)
    }
  }

  it should "correctly accept registrations from a link-local address" in {
    ssoSuccessfulRegistrationAndLink()

    import driver.simple._

    val addr = Some(RemoteAddress("169.254.0.1"))

    whenReady(registrationService.registerUser(simpleReg, addr)) { token =>
      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }
      assertUserRegistered(token, regId)
    }
  }

  it should "correctly accept registrations from a private address" in {
    ssoSuccessfulRegistrationAndLink()

    import driver.simple._

    val addr = Some(RemoteAddress("192.168.0.1"))

    whenReady(registrationService.registerUser(simpleReg, addr)) { token =>
      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }
      assertUserRegistered(token, regId)
    }
  }

  it should "correctly accept registrations from the UK" in {
    ssoSuccessfulRegistrationAndLink()

    import driver.simple._

    val addr = Some(RemoteAddress("81.168.77.149")) // NTP server in Falmouth (UK)

    whenReady(registrationService.registerUser(simpleReg, addr)) { token =>
      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }
      assertUserRegistered(token, regId)
    }
  }

  it should "correctly accept registrations from the IE" in {
    ssoSuccessfulRegistrationAndLink()

    import driver.simple._

    val addr = Some(RemoteAddress("78.143.174.10")) // NTP server ie.pool.ntp.org

    whenReady(registrationService.registerUser(simpleReg, addr)) { token =>
      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first.value }
      assertUserRegistered(token, regId)
    }
  }
}
