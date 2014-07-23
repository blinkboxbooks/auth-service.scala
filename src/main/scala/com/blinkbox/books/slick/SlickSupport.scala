package com.blinkbox.books.slick

import org.joda.time.{DateTimeZone, DateTime}

import scala.slick.driver.{JdbcProfile, H2Driver, JdbcDriver, MySQLDriver}
import scala.slick.profile._

trait SlickSupport[Profile <: BasicProfile] {
  val driver: Profile
  type Session = Profile#Backend#Session
}

trait JdbcSupport extends SlickSupport[JdbcProfile] {
  import driver.simple._

  protected implicit def jodaDateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime, DateTimeZone.UTC))
}
