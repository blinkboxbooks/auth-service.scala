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

object TestGeoIP {
  def geoIpStub(stubValue: String = "GB") = new GeoIP {
    override def countryCode(address: RemoteAddress): String = stubValue
  }
}

class PublisherSpy extends Publisher {
  var events = List.empty[Event]

  override def publish(event: Event): Future[Unit] = {
    events ::= event
    Future.successful(())
  }
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
