package com.blinkbox.books.auth.server.env

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait SpecBase extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: Suite =>

  val env = new TestEnv

  override protected def afterEach(): Unit = {
    super.afterEach()
    env.cleanup()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    env.actorSystem.shutdown()
  }
}
