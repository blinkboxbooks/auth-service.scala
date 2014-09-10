package com.blinkbox.books.testkit

import com.blinkbox.books.auth.server.events.{Event, Publisher}
import com.blinkbox.books.auth.server.services.GeoIP
import spray.http.RemoteAddress

import scala.concurrent.Future

class PublisherSpy extends Publisher {
  var events = List.empty[Event]

  override def publish(event: Event): Future[Unit] = {
    events ::= event
    Future.successful(())
  }
}
