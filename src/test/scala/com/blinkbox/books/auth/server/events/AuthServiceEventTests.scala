package com.blinkbox.books.auth.server.events

import com.blinkbox.books.auth.server.data.{Client, JdbcAuthRepository, User}
import com.blinkbox.books.auth.server.{DefaultAuthService, GeoIP, UserRegistration}
import com.blinkbox.books.slick.H2Support
import com.blinkbox.books.time.{Clock, StoppedClock, TimeSupport}
import org.hamcrest.{Description, BaseMatcher, Matcher}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.FunSuite
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}
import spray.http.RemoteAddress
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

object TestGeoIP extends GeoIP {
  override def countryCode(address: RemoteAddress): String = "GB"
}

class H2AuthRepository(implicit val clock: Clock) extends JdbcAuthRepository with H2Support with TimeSupport {
  import driver.simple._
  val db = {
    val database = driver.backend.Database.forURL("jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1", driver = driverName)
    database.withSession { implicit session =>
      (users.ddl ++ clients.ddl ++ refreshTokens.ddl ++ loginAttempts.ddl).create
    }
    database
  }
}

trait ExtraMockitoSugar extends MockitoSugar {
  import scala.language.implicitConversions

  implicit def func2answer[T](f: => T): Answer[T] = new Answer[T] {
    override def answer(invocation: InvocationOnMock): T = f
  }

  implicit def func2matcher[T](f: T => Boolean): Matcher[T] = new BaseMatcher[T] {
    override def matches(item: Any): Boolean = f(item.asInstanceOf[T])
    override def describeTo(description: Description): Unit = description.appendText("custom matcher")
  }
}

class AuthServiceEventTests extends FunSuite with ScalatestRouteTest with AsyncAssertions with ExtraMockitoSugar {

  implicit val clock = StoppedClock()
  implicit val patience = PatienceConfig(scaled(Span(500, Millis))) // the h2 tests can take a little while

  test("A user registered notification is sent on user registration") {
    val w = new Waiter
    val publisher = mock[Publisher]
    when(publisher.userRegistered(any[User], any[Option[Client]])).thenAnswer(Future.successful(w.dismiss()))

    val reg = UserRegistration("John", "Doe", "johndoe@example.org", "password", acceptedTerms = true, allowMarketing = true, None, None, None, None)
    val authService = new DefaultAuthService(new H2AuthRepository, TestGeoIP, publisher)
    authService.registerUser(reg, None)
    w.await()

    val matchesUser = (u: User) => u.username == reg.username && u.firstName == reg.firstName
    verify(publisher).userRegistered(argThat(matchesUser), any[Option[Client]])
  }


}
