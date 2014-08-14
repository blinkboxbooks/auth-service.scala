package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.{InvalidClient, InvalidGrant, InvalidRequest}
import com.blinkbox.books.auth.server.data.{UserId, User}
import com.blinkbox.books.auth.server.env.{RegistrationResponder, UserInfoResponder, AuthenticationTestEnv}
import com.blinkbox.books.auth.server.events.{UserUpdated, UserAuthenticated, UserRegistered}
import com.blinkbox.books.auth.server.sso.SSOCredentials
import com.blinkbox.books.auth.server._
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import spray.http.{HttpEntity, StatusCodes, HttpResponse}

import scala.concurrent.duration.FiniteDuration

class DefaultPasswordAuthenticationServiceSpecs  extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  val dummyCreds = PasswordCredentials("foo", "bar", None, None)

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
    failingWith[ZuulRequestException](passwordAuthenticationService.authenticate(dummyCreds, None)) should matchPattern {
      case ZuulRequestException(_, InvalidGrant, None) =>
    }
  }

  it should "not create an access token and signal an error when providing correct username/password pairs but wrong client details" in new AuthenticationTestEnv {
    ssoSuccessfulAuthentication()
    failingWith[ZuulRequestException](passwordAuthenticationService.authenticate(PasswordCredentials("user.a@test.tst", "a-password", Some("foo"), Some("bar")), None)) should matchPattern {
      case ZuulRequestException(_, InvalidClient, None) =>
    }
  }

  it should "not create an access token and signal an error when the SSO service answers with a bad-request error" in new AuthenticationTestEnv {
    val err = "Invalid username or password"
    ssoInvalidRequest(err)

    failingWith[ZuulRequestException](passwordAuthenticationService.authenticate(dummyCreds, None)) should matchPattern {
      case ZuulRequestException(m, InvalidRequest, None) if m == err =>
    }
  }

  it should "not create an access token and signal an error when the SSO service throttles requests" in new AuthenticationTestEnv {
    ssoTooManyRequests(10)

    failingWith[ZuulTooManyRequestException](passwordAuthenticationService.authenticate(dummyCreds, None)) should matchPattern {
      case ZuulTooManyRequestException(_, d) if d.toSeconds == 10 =>
    }
  }

  it should "create an access token and register an user in the system if the SSO returns a success but we don't have an user in our database" in new AuthenticationTestEnv with UserInfoResponder {
    ssoSuccessfulAuthentication()
    ssoSuccessfulUserInfo()
    ssoResponse.complete(_.success(HttpResponse(StatusCodes.NoContent, HttpEntity.Empty))) // Link request

    whenReady(passwordAuthenticationService.authenticate(PasswordCredentials("john.doe+blinkbox@example.com", "foobar", None, None), None)) { _ =>
      val userInDb = db.withSession { implicit session => userRepository.userWithUsername("john.doe+blinkbox@example.com") }

      userInDb shouldBe defined
      userInDb foreach { u =>
        publisherSpy.events should equal(UserAuthenticated(u, None) :: UserRegistered(u) :: Nil)
      }
    }
  }

  it should "create an access token and link an user if the SSO returns a success but the user in our database is not yet linked" in new AuthenticationTestEnv with UserInfoResponder {
    ssoSuccessfulAuthentication()
    ssoResponse.complete(_.success(HttpResponse(StatusCodes.NoContent, HttpEntity.Empty))) // Link request
    ssoSuccessfulUserInfo()

    val username = "john.doe+blinkbox@example.com"

    import driver.simple._
    db.withSession { implicit session => tables.users += User(UserId.Invalid, clock.now(), clock.now(), username, "Foo", "Bar", "some-hash", true, None) }

    val notLinkedUser = db.withSession { implicit session => userRepository.userWithUsername(username) } getOrElse(sys.error("Expected user in DB"))

    whenReady(passwordAuthenticationService.authenticate(PasswordCredentials(username, "foobar", None, None), None)) { _ =>
      val userInDb = db.withSession { implicit session => userRepository.userWithUsername(username) }

      userInDb shouldBe defined
      userInDb foreach { u =>
        u.ssoId shouldBe defined
        publisherSpy.events should equal(UserAuthenticated(u, None) :: UserUpdated(notLinkedUser, u) :: Nil)
      }
    }
  }
}
