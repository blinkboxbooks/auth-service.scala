package com.blinkbox.books.auth.server.events

import com.blinkbox.books.auth.server.test.H2AuthRepository
import com.blinkbox.books.auth.server.{DefaultAuthService, GeoIP, UserRegistration}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.StoppedClock
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Millis, Span}
import spray.http.RemoteAddress
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

private object TestGeoIP extends GeoIP {
  override def countryCode(address: RemoteAddress): String = "GB"
}

class AuthServiceEventTests extends FunSuite with ScalatestRouteTest with AsyncAssertions with MockitoSyrup {

  implicit val clock = StoppedClock()
  implicit val patience = PatienceConfig(scaled(Span(500, Millis))) // the h2 tests can take a little while

  test("A user registered notification is sent on user registration") {
    val w = new Waiter
    val publisher = mock[Publisher]
    when(publisher.publish(any[Event])).thenAnswer(Future.successful(w.dismiss()))

    val reg = UserRegistration("John", "Doe", "johndoe@example.org", "password", acceptedTerms = true, allowMarketing = true, None, None, None, None)
    val authService = new DefaultAuthService(new H2AuthRepository, TestGeoIP, publisher)
    authService.registerUser(reg, None)
    w.await()

    verify(publisher).publish(any[UserRegistered]) // TODO: Check the user etc.
  }

  // TODO: Rest of the tests

}
