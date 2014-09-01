package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.cake.{DefaultAsyncComponent, DefaultConfigComponent}
import com.blinkbox.books.auth.server.env.{TestConfigComponent, TestSsoComponent}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class SsoTestEnv
    extends DefaultAsyncComponent
    with TestConfigComponent
    with TestSsoComponent

trait SpecBase extends ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))
}
