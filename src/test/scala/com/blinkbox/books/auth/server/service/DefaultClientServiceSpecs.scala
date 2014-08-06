package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.ZuulRequestErrorReason.ClientLimitReached
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.env.TestEnv
import com.blinkbox.books.auth.server.events.{ClientDeregistered, ClientUpdated}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

class DefaultClientServiceSpecs extends FlatSpec with Matchers with ScalaFutures with FailHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))

  import com.blinkbox.books.testkit.TestH2.tables._
  import driver.simple._

  "The user service" should "list active clients for registered users" in new TestEnv {
    whenReady(clientService.listClients()(authenticatedUserA)) { list =>
      list.clients should have length(2)
      list.clients should matchPattern { case clientIdA :: clientIdB :: Nil => }
    }

    whenReady(clientService.listClients()(authenticatedUserB)) { list =>
      list.clients shouldBe empty
    }
  }

  it should "access to client details by id only for the owning users and non-deregistered clients" in new TestEnv {
    whenReady(clientService.getClientById(clientIdA1)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA1) }
    }

    whenReady(clientService.getClientById(clientIdA2)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA2) }
    }

    forbiddenClientAccesses foreach { case (c, u) =>
      whenReady(clientService.getClientById(c)(u)) { infoOpt =>
        infoOpt shouldBe empty
      }
    }
  }

  it should "delete clients and revoke their tokens by id given they are owned by the user and not already de-registered" in new TestEnv {
    whenReady(clientService.deleteClient(clientIdA1)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA1) }
      publisherSpy.events should equal(ClientDeregistered(clientA1.copy(isDeregistered = true)) :: Nil)

      db.withSession { implicit session =>
        tables.refreshTokens.where(_.clientId === clientIdA1).map(_.isRevoked).foreach(_ shouldBe true)
      }
    }

    whenReady(clientService.deleteClient(clientIdA2)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA2) }
      publisherSpy.events should equal(
        ClientDeregistered(clientA2.copy(isDeregistered = true)) ::
        ClientDeregistered(clientA1.copy(isDeregistered = true)) ::Nil)

      db.withSession { implicit session =>
        tables.refreshTokens.where(_.clientId === clientIdA2).map(_.isRevoked).foreach(_ shouldBe true)
      }
    }
  }

  it should "not allow deletion of non-owned, non-existing or already-deregistered clients" in new TestEnv {
    forbiddenClientAccesses foreach { case (c, u) =>
      whenReady(clientService.deleteClient(c)(u)) { infoOpt =>
        infoOpt shouldBe empty
        publisherSpy.events shouldBe empty
      }
    }
  }

  it should "apply a full patch to owned non-deregistered clients" in new TestEnv {
    whenReady(clientService.updateClient(clientIdA1, fullClientPatch)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(fullPatchedClientInfoA1) }

      db.withSession { implicit session =>
        val storedClient = tables.clients.where(_.id === clientIdA1).firstOption
        storedClient shouldBe defined
        storedClient.foreach { _ should equal(fullPatchedClientA1) }
      }

      publisherSpy.events should equal(ClientUpdated(clientA1, fullPatchedClientA1) :: Nil)
    }
  }

  it should "not allow updating non-owned, non-existing or non-deregistered clients" in new TestEnv {
    forbiddenClientAccesses foreach { case (c, u) =>
      whenReady(clientService.updateClient(c, fullClientPatch)(u)) { infoOpt =>
        infoOpt shouldBe empty
        publisherSpy.events shouldBe empty
      }
    }
  }

  it should "allow creating a new client for users below their limits" in new TestEnv {
    whenReady(clientService.registerClient(clientRegistration)(authenticatedUserB)) { info =>
      val lastClient = db.withSession { implicit session =>
        tables.clients.sortBy(_.id.desc).first()
      }

      lastClient.name should equal("Test name")
      lastClient.brand should equal("Test brand")
      lastClient.model should equal("Test model")
      lastClient.os should equal("Test OS")

      val id = lastClient.id.value
      val secret = lastClient.secret

      info should equal(ClientInfo(
        ClientId(id).external, s"/clients/$id", "Test name", "Test brand", "Test model", "Test OS", Some(secret), now))
    }

  }

  it should "respect client limits for an user preventing the creation of more clients" in new TestEnv {
    failingWith[ZuulRequestException](clientService.registerClient(clientRegistration)(authenticatedUserC)) should matchPattern {
      case ZuulRequestException(_, InvalidRequest, Some(ClientLimitReached)) =>
    }

    db.withSession { implicit session =>
      tables.clients.where(_.id === ClientId(4)).map(_.isDeregistered).update(true)
    }

    whenReady(clientService.registerClient(clientRegistration)(authenticatedUserC)) { _ => }
  }
}

