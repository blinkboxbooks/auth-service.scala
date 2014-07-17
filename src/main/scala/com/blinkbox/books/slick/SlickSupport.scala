package com.blinkbox.books.slick

import org.joda.time.DateTime

import scala.slick.driver.{H2Driver, JdbcDriver, MySQLDriver}
import scala.slick.profile.{BasicDriver, RelationalDriver, SqlDriver}

trait SlickSupport {
  protected val driverName: String
  protected val driver: BasicDriver
  type Session = driver.backend.Session
  val db: driver.backend.Database
}

trait RelationalSupport extends SlickSupport {
  protected val driver: RelationalDriver
}

trait SqlSupport extends RelationalSupport {
  protected val driver: SqlDriver
}

trait JdbcSupport extends SqlSupport {
  protected val driver: JdbcDriver
  import driver.simple._
  protected implicit def jodaDateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime))
}

trait MySqlSupport extends JdbcSupport {
  protected val driverName: String = "com.mysql.jdbc.Driver"
  protected val driver: MySQLDriver = MySQLDriver
}

trait H2Support extends JdbcSupport {
  protected val driverName: String = "org.h2.Driver"
  protected val driver: H2Driver = H2Driver
}
