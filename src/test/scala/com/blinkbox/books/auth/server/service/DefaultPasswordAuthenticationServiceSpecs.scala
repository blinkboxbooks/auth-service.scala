package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidClient, InvalidGrant}
import com.blinkbox.books.auth.server.{ZuulRequestException, PasswordCredentials}
import com.blinkbox.books.auth.server.env.AuthenticationTestEnv
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

class DefaultPasswordAuthenticationServiceSpecs  extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  "The password authentication service" should "create an access token for valid user credentials without a client and not providing an IP" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()
    whenReady(passwordAuthenticationService.authenticate(PasswordCredentials("user.a@test.tst", "a-password", None, None), None)) { token =>
      token.user_first_name should equal(userA.firstName)
      token.user_last_name should equal(userA.lastName)
      token.user_username should equal(userA.username)
      token.user_id should equal(userA.id.external)

      token.client_id shouldBe empty
      token.client_brand shouldBe empty
      token.client_model shouldBe empty
      token.client_name shouldBe empty
      token.client_os shouldBe empty
      token.client_secret shouldBe empty
      token.client_uri shouldBe empty
    }
  }

  it should "create an access token for valid user credentials with a client and not providing an IP" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()
    whenReady(passwordAuthenticationService.authenticate(
      PasswordCredentials("user.a@test.tst", "a-password", Some(clientInfoA1.client_id), Some("test-secret-a1")), None)) { token =>

      token.user_first_name should equal(userA.firstName)
      token.user_last_name should equal(userA.lastName)
      token.user_username should equal(userA.username)
      token.user_id should equal(userA.id.external)

      token.client_id shouldBe Some(clientInfoA1.client_id)
      token.client_brand shouldBe Some(clientInfoA1.client_brand)
      token.client_model shouldBe Some(clientInfoA1.client_model)
      token.client_name shouldBe Some(clientInfoA1.client_name)
      token.client_os shouldBe Some(clientInfoA1.client_os)
      token.client_secret shouldBe None
      token.client_uri shouldBe Some(clientInfoA1.client_uri)
    }
  }

  it should "not create an access token and signal an error when providing wrong username/password pairs" in new AuthenticationTestEnv {
    ssoUnsuccessfulAuthentication()
    failingWith[ZuulRequestException](passwordAuthenticationService.authenticate(PasswordCredentials("foo", "bar", None, None), None)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "not create an access token and signal an error when providing correct username/password pairs but wrong client details" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()
    failingWith[ZuulRequestException](passwordAuthenticationService.authenticate(PasswordCredentials("user.a@test.tst", "a-password", Some("foo"), Some("bar")), None)) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }
}
