package com.blinkbox.books.slick

import org.joda.time.{DateTime, DateTimeZone}

import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile
import scala.slick.profile._

trait SlickTypes[Profile <: BasicProfile] {
  type Session = Profile#Backend#Session
  type Database = Profile#Backend#Database
}

trait TablesContainer[+Profile <: JdbcProfile] {
  val driver: Profile

  import driver.simple._

  protected implicit def jodaDateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime, DateTimeZone.UTC))
}

trait TablesSupport[Profile <: JdbcProfile, Tables <: TablesContainer[Profile]] {
  val tables: Tables
}

