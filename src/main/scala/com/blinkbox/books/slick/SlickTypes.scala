package com.blinkbox.books.slick

import java.sql.SQLException

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException
import org.joda.time.{DateTime, DateTimeZone}

import scala.slick.driver.JdbcProfile
import scala.slick.profile._

sealed abstract class DatabaseException[T <: SQLException](cause: T) extends Exception(cause.getMessage, cause)
case class ConstraintException[T <: SQLException](cause: T) extends DatabaseException[T](cause)
case class UnknownDatabaseException[T <: SQLException](cause: T) extends DatabaseException[T](cause)

/**
 * Utility to mix in to get type alias for classes depending on a specific slick profile
 */
trait SlickTypes[Profile <: BasicProfile] {
  type Session = Profile#Backend#Session
  type Database = Profile#Backend#Database
}

/**
 * Trait that provides support for a specific profile; ideal to mix in a wrapper for slick tables definitions
 */
trait TablesContainer[Profile <: JdbcProfile] {
  val driver: Profile

  import driver.simple._

  protected implicit def jodaDateTimeColumnType = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new java.sql.Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime, DateTimeZone.UTC))
}

/**
 * Trait to mix in to signal that an implementation of the TablesContainer is needed for the proper Profile
 */
trait TablesSupport[Profile <: JdbcProfile, Tables <: TablesContainer[Profile]] {
  val tables: Tables
}

/**
 * Basic types used in the subsequent components to remain db-agnostic
 */
trait DatabaseSupport {
  type Session = Profile#Backend#Session
  type Profile <: JdbcProfile
  type Database = Profile#Backend#Database

  protected type ExceptionTransformer = PartialFunction[Throwable, DatabaseException[_ <: SQLException]]
  protected def exceptionTransformer: ExceptionTransformer

  object ExceptionFilter {
    def apply(f: PartialFunction[Throwable, Throwable]) = exceptionTransformer andThen f orElse f
  }

  type ExceptionFilter = ExceptionFilter.type
}

/**
 * Base component to be implemented for a profile-parametrized database connection. Please note that the constraintExceptionTag
 * is quite tricky and has to be implemented by instantiating the correct DBTypes implementation.
 */
trait BaseDatabaseComponent {
  val DB: DatabaseSupport
  type Tables <: TablesContainer[DB.Profile]

  def driver: DB.Profile
  def db: DB.Database
  def tables: Tables

  lazy val exceptionFilter = DB.ExceptionFilter
}

/**
 * Stub for implementing repository components
 */
trait BaseRepositoriesComponent {
  this: BaseDatabaseComponent =>
}

class MySQLDatabaseSupport extends DatabaseSupport {
  type Profile = JdbcProfile

  override def exceptionTransformer = {
    case ex: MySQLIntegrityConstraintViolationException => ConstraintException(ex)
  }
}

class H2DatabaseSupport extends DatabaseSupport {
  type Profile = JdbcProfile

  override def exceptionTransformer = {
    case ex: org.h2.jdbc.JdbcSQLException => ConstraintException(ex)
  }
}
