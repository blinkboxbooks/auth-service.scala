package com.blinkbox.books.testkit

import java.util.concurrent.atomic.AtomicReference

import com.blinkbox.books.auth.server.events.{Event, Publisher}
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.services.GeoIP
import org.h2.jdbc.JdbcSQLException
import org.scalatest.Assertions
import org.scalatest.concurrent.{AsyncAssertions, PatienceConfiguration}
import spray.http.RemoteAddress

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend.Database
import scala.util.{Try, Success, Failure}

object TestH2 {
  val tables = ZuulTables(H2Driver)

  def db = {
    val threadId = Thread.currentThread().getId()
    val database = Database.forURL(s"jdbc:h2:mem:auth$threadId;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    import tables.driver.simple._

    database.withSession { implicit session =>
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

trait FailHelper extends Assertions with AsyncAssertions with PatienceConfiguration {
  def failingWith[T <: Throwable : Manifest](f: Future[_])(implicit p: PatienceConfig, ec: ExecutionContext): T = {
    val at = new AtomicReference[Try[Any]]()

    val w = new Waiter
    f onComplete {
      case Success(i) =>
        at.set(Success(i))
        w.dismiss()
      case Failure(e) =>
        at.set(Failure(e))
        w.dismiss()
    }
    w.await()(p)

    at.get() match {
      case Success(i) => intercept[T](i)
      case Failure(e) => intercept[T](throw e)
    }
  }
}
