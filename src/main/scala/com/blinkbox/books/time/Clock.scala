package com.blinkbox.books.time

import org.joda.time.{DateTime, DateTimeZone}

/**
 * A clock that gives access to the current UTC time.
 */
trait Clock {
  def now(): DateTime
}

/**
 * A clock that gives access to the current UTC system time.
 */
object SystemClock extends Clock {
  def now() = DateTime.now(DateTimeZone.UTC)
}

/**
 * A clock that always returns a specified date and time.
 * @param stoppedAt The date and time that the clock stopped at.
 */
case class StoppedClock(stoppedAt: DateTime) extends Clock {
  def now() = stoppedAt
}

/**
 * Provides methods to construct stopped clocks.
 */
object StoppedClock {
  def apply: StoppedClock = StoppedClock(SystemClock.now())
}

/**
 * Provides an implicitly available clock.
 */
trait TimeSupport {
  implicit val clock: Clock
}

/**
 * Provides an implicitly available system clock.
 */
trait SystemTimeSupport extends TimeSupport {
  implicit val clock: Clock = SystemClock
}