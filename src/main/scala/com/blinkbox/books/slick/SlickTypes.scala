package com.blinkbox.books.slick

import org.joda.time.{DateTime, DateTimeZone}

import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile
import scala.slick.profile._

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
trait DBTypes {
  type Profile <: JdbcProfile
  type ConstraintException <: Throwable
  type Database = Profile#Backend#Database
  val constraintExceptionTag: ClassTag[ConstraintException]
}

/**
 * Base component to be implemented for a profile-parametrized database connection. Please note that the constraintExceptionTag
 * is quite tricky and has to be implemented by instantiating the correct DBTypes implementation.
 */
trait BaseDatabaseComponent {
  val Types: DBTypes
  type Tables <: TablesContainer[Types.Profile]

  def driver: Types.Profile
  def db: Types.Database
  def tables: Tables

  implicit lazy val constraintExceptionTag: ClassTag[Types.ConstraintException] = Types.constraintExceptionTag
}

/**
 * Stub for implementing repository components
 */
trait BaseRepositoriesComponent {
  this: BaseDatabaseComponent =>
}
