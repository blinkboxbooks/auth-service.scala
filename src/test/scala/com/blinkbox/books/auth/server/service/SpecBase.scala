package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.env
import com.blinkbox.books.test.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.language.experimental.macros

class SpecBase extends FlatSpec with env.SpecBase with Matchers with ScalaFutures with FailHelper {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
}
