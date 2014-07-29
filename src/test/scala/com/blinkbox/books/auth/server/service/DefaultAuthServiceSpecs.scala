package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidClient, InvalidGrant}
import com.blinkbox.books.auth.server._
import com.blinkbox.books.testkit.FailHelper
import com.blinkbox.books.time.StoppedClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

// TODO: IP-related scenarios are not being tested at the moment, add those tests
class DefaultAuthServiceSpecs extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val cl = StoppedClock()
  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  val simpleReg = UserRegistration("New First", "New Last", "new.user@test.tst", "new-password", true, true, None, None, None, None)
  val clientReg = UserRegistration("New First", "New Last", "new.user@test.tst", "new-password", true, true,
    Some("New Name"), Some("New Brand"), Some("New Model"), Some("New OS"))

  def assertUserRegistered(token: TokenInfo, regId: Int): Unit = {
    token.user_first_name should equal("New First")
    token.user_last_name should equal("New Last")
    token.user_username should equal("new.user@test.tst")
    token.user_id should equal(s"urn:blinkbox:zuul:user:$regId")
    token.user_uri should equal(s"/users/$regId")
  }

  "The auth service" should "register a user without a client and no IP" in new DefaultH2TestEnv {
    whenReady(authService.registerUser(simpleReg, None)) { token =>
      import driver.simple._

      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first().value }

      assertUserRegistered(token, regId)
      token.refresh_token shouldBe defined
      token.client_name shouldBe empty
      token.client_brand shouldBe empty
      token.client_model shouldBe empty
      token.client_os shouldBe empty
    }
  }

  it should "register a user with a client" in new DefaultH2TestEnv {
    whenReady(authService.registerUser(clientReg, None)) { token =>
      import driver.simple._

      val regId = db.withSession { implicit session => tables.users.sortBy(_.id.desc).map(_.id).first().value }

      assertUserRegistered(token, regId)
      token.refresh_token shouldBe defined
      token.client_name shouldBe Some("New Name")
      token.client_brand shouldBe Some("New Brand")
      token.client_model shouldBe Some("New Model")
      token.client_os shouldBe Some("New OS")
    }
  }

  it should "revoke a valid refresh token" in new DefaultH2TestEnv {
    whenReady(authService.revokeRefreshToken(refreshTokenClientA1.token)) { _ =>  }
  }

  it should "signal an error when revoking an invalid refresh token" in new DefaultH2TestEnv {
    failingWith[ZuulRequestException](authService.revokeRefreshToken("foo-token")) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "signal an error when revoking an already revoked refresh token" in new DefaultH2TestEnv {
    failingWith[ZuulRequestException](authService.revokeRefreshToken(refreshTokenClientA3.token)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "create an access token for valid user credentials without a client and not providing an IP" in new DefaultH2TestEnv {
    whenReady(authService.authenticate(PasswordCredentials("user.a@test.tst", "a-password", None, None), None)) { token =>
      token.user_first_name should equal(userA.firstName)
      token.user_last_name should equal(userA.lastName)
      token.user_username should equal(userA.username)
      token.user_id should equal(s"urn:blinkbox:zuul:user:${userA.id.value}")

      token.client_id shouldBe empty
      token.client_brand shouldBe empty
      token.client_model shouldBe empty
      token.client_name shouldBe empty
      token.client_os shouldBe empty
      token.client_secret shouldBe empty
      token.client_uri shouldBe empty
    }
  }

  it should "create an access token for valid user credentials with a client and not providing an IP" in new DefaultH2TestEnv {
    whenReady(authService.authenticate(
      PasswordCredentials("user.a@test.tst", "a-password", Some(clientInfoA1.client_id), Some("test-secret-a1")), None)) { token =>

      token.user_first_name should equal(userA.firstName)
      token.user_last_name should equal(userA.lastName)
      token.user_username should equal(userA.username)
      token.user_id should equal(s"urn:blinkbox:zuul:user:${userA.id.value}")

      token.client_id shouldBe Some(clientInfoA1.client_id)
      token.client_brand shouldBe Some(clientInfoA1.client_brand)
      token.client_model shouldBe Some(clientInfoA1.client_model)
      token.client_name shouldBe Some(clientInfoA1.client_name)
      token.client_os shouldBe Some(clientInfoA1.client_os)
      token.client_secret shouldBe None
      token.client_uri shouldBe Some(clientInfoA1.client_uri)
    }
  }

  it should "not create an access token and signal an error when providing wrong username/password pairs" in new DefaultH2TestEnv {
    failingWith[ZuulRequestException](authService.authenticate(PasswordCredentials("foo", "bar", None, None), None)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "not create an access token and signal an error when providing correct username/password pairs but wrong client details" in new DefaultH2TestEnv {
    failingWith[ZuulRequestException](authService.authenticate(PasswordCredentials("user.a@test.tst", "a-password", Some("foo"), Some("bar")), None)) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  //def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]

  it should "refresh a valid refresh token given the associated client credentials" in new DefaultH2TestEnv {
    val refreshFuture = authService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA1.token, Some(clientInfoA1.client_id), Some("test-secret-a1")))

    whenReady(refreshFuture) { token =>
      token.expires_in should equal(1800)
      token.user_id shouldBe s"urn:blinkbox:zuul:user:${userIdA.value}"
      token.client_id shouldBe Some(clientInfoA1.client_id)

      import tables._
      import tables.driver.simple._
      val updatedToken = db.withSession { implicit session =>
        tables.refreshTokens.where(_.id === refreshTokenClientA1.id).firstOption
      }

      updatedToken shouldBe defined
      updatedToken.foreach(_.expiresAt should equal(now.plusDays(90)))
    }
  }

  it should "not refresh a valid refresh token and signal an error if wrong client credentials are provided" in new DefaultH2TestEnv {
    val refreshFuture = authService.refreshAccessToken(
      RefreshTokenCredentials(refreshTokenClientA2.token, Some(clientInfoA1.client_id), Some("test-secret-a2")))

    failingWith[ZuulRequestException](refreshFuture) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  it should "not refresh an invalid refresh token and signal an error whether or not correct client credentials are provided" in new DefaultH2TestEnv {
    val correctClientFuture = authService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", Some(clientInfoA1.client_id), Some("test-secret-a1")))

    val noClientFuture = authService.refreshAccessToken(
      RefreshTokenCredentials("foo-token", None, None))

    (correctClientFuture :: noClientFuture :: Nil) foreach { f =>
      failingWith[ZuulRequestException](f) should matchPattern {
        case ZuulRequestException(_, InvalidGrant, None) =>
      }
    }
  }
}
