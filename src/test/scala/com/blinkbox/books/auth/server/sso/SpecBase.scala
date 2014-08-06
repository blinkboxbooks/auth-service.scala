package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.cake.{DefaultAsyncComponent, DefaultConfigComponent}
import com.blinkbox.books.auth.server.env.TestSSOComponent
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class SSOTestEnv
    extends DefaultAsyncComponent
    with DefaultConfigComponent
    with TestSSOComponent

trait SpecBase extends ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))
}
