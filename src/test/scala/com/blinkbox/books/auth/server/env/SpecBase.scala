package com.blinkbox.books.auth.server.env

import org.scalatest._

trait SpecBase extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: Suite =>

  val env = new TestEnv

  override protected def afterEach(): Unit = {
    super.afterEach()
    env.cleanup()
  }

  abstract override def withFixture(test: NoArgTest) = {
    super.withFixture(test) match {
      case Succeeded =>
        if (env.ssoResponse.isDone) Succeeded
        else Failed("Expected SSO invocations, none received")
      case x => x
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    env.actorSystem.shutdown()
  }
}
