package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env
import com.blinkbox.books.test.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class SpecBase extends FlatSpec with env.SpecBase with Matchers with ScalaFutures with FailHelper {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
}
