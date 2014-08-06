package com.blinkbox.books.slick

import org.joda.time.{DateTime, DateTimeZone}

import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile
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

trait DatabaseTypes {
  type Profile <: BasicProfile
  type ConstraintException <: Throwable
  type Database = Profile#Backend#Database
  lazy implicit val constraintExceptionTag: ClassTag[ConstraintException] = scala.reflect.classTag[ConstraintException]
}
