package com.blinkbox.books.auth.server

import org.joda.time.{DateTime, DateTimeZone}

trait Clock {
  def now(): DateTime
}

object SystemClock extends Clock {
  def now() = DateTime.now(DateTimeZone.UTC)
}

trait TimeSupport {
  implicit val clock: Clock
}

trait SystemTimeSupport extends TimeSupport {
  implicit val clock: Clock = SystemClock
}