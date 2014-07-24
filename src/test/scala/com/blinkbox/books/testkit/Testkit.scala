package com.blinkbox.books.testkit

import com.blinkbox.books.auth.server.events.{Event, Publisher}
import com.blinkbox.books.auth.server.{PasswordHasher, GeoIP}
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.slick.JdbcSupport
import org.h2.jdbc.JdbcSQLException
import org.hamcrest.{BaseMatcher, Description, Matcher}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import spray.http.RemoteAddress

import scala.concurrent.Future
import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend.Database

object H2 {
  val tables = new AuthTables with JdbcSupport { val driver = H2Driver }

  def db = {
    val threadId = Thread.currentThread().getId()
    val database = Database.forURL(s"jdbc:h2:mem:auth$threadId;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    database.withSession { implicit session =>
      import tables.driver.simple._

      val ddl = (tables.users.ddl ++ tables.clients.ddl ++ tables.refreshTokens.ddl ++ tables.loginAttempts.ddl)

      try {
        ddl.drop
      } catch { case _: JdbcSQLException => /* Do nothing */ }

      ddl.create
    }

    database
  }
}

object TestGeoIP {
  def geoIpStub(stubValue: String = "GB") = new GeoIP {
    override def countryCode(address: RemoteAddress): String = stubValue
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

class PublisherSpy extends Publisher {
  var events = List.empty[Event]

  override def publish(event: Event): Future[Unit] = {
    events ::= event
    Future.successful()
  }
}

object PublisherDummy extends Publisher {
  override def publish(event: Event): Future[Unit] = Future.successful()
}
