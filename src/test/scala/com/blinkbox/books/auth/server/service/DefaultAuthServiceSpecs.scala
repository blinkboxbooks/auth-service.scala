package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.PasswordHasher
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.services.DefaultAuthService
import com.blinkbox.books.testkit.{PublisherSpy, TestGeoIP, TestH2}
import com.blinkbox.books.time.StoppedClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

class DefaultAuthServiceSpecs extends FlatSpec with Matchers with ScalaFutures {

  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val cl = StoppedClock()
  implicit override val patienceConfig = PatienceConfig(timeout = Span(500, Millis), interval = Span(20, Millis))

  val tables = TestH2.tables

  trait TestEnv {
    val db = TestH2.db
    val publisherSpy = new PublisherSpy
    val userRepository = new DefaultUserRepository(tables, PasswordHasher(identity))
    val authRepository = new DefaultAuthRepository(tables)
    val clientRepository = new DefaultClientRepository(tables)
    val userService = new DefaultAuthService(TestH2.db, authRepository, userRepository, clientRepository, TestGeoIP.geoIpStub(), publisherSpy)

    val dummyUser = User(UserId(1), cl.now(), cl.now(), "dummy@dummy.dm", "Dummy", "Dummy", "dummypwd", true)

    import tables.driver.simple._

    db.withSession { implicit session =>
      tables.users += dummyUser
    }
  }

  //def registerUser(registration: UserRegistration, clientIP: Option[RemoteAddress]): Future[TokenInfo]

  "The auth service" should "register a user without a client" in new TestEnv {

  }

  "The auth service" should "register a user with a client" in new TestEnv {

  }

  //def revokeRefreshToken(token: String): Future[Unit]

  "The auth service" should "revoke a valid refresh token" in new TestEnv {

  }

  "The auth service" should "signal an error when revoking an invalid refresh token" in new TestEnv {

  }

  //def authenticate(credentials: PasswordCredentials, clientIP: Option[RemoteAddress]): Future[TokenInfo]

  "The auth service" should "create an access token for valid user credentials without a client" in new TestEnv {

  }

  "The auth service" should "create an access token for valid user credentials with a client" in new TestEnv {

  }

  "The auth service" should "not create an access token and signal an error when providing incomplete client credentials" in new TestEnv {

  }

  "The auth service" should "not create an access token and signal an error when providing wrong username/password pairs" in new TestEnv {

  }

  //def refreshAccessToken(credentials: RefreshTokenCredentials): Future[TokenInfo]

  "The auth service" should "refresh a valid refresh token" in new TestEnv {

  }

  "The auth service" should "not refresh an invalid refresh token and signal an error" in new TestEnv {

  }

  //def querySession()(implicit user: AuthenticatedUser): Future[SessionInfo]

  "The auth service" should "provide session information for an authenticated user" in new TestEnv {

  }

  "The auth service" should "not allow access to another user's session using a tampered token" in new TestEnv {

  }
}
