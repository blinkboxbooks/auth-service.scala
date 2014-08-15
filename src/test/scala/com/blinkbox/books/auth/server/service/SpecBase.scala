package com.blinkbox.books.auth.server.service

import com.blinkbox.books.testkit.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Millis}
import org.scalatest.{Matchers, FlatSpec}

class SpecBase  extends FlatSpec with Matchers with ScalaFutures with FailHelper {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
}
