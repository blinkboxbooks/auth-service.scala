package com.blinkbox.books.auth.server.test

import com.blinkbox.books.auth.server.data.JdbcAuthRepository
import com.blinkbox.books.slick.H2Support
import com.blinkbox.books.time.{Clock, TimeSupport}

class H2AuthRepository(implicit val clock: Clock) extends JdbcAuthRepository with H2Support with TimeSupport {
  import driver.simple._
  val db = {
    val database = driver.backend.Database.forURL("jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1", driver = driverName)
    database.withSession { implicit session =>
      (users.ddl ++ clients.ddl ++ refreshTokens.ddl ++ loginAttempts.ddl).create
    }
    database
  }
}
