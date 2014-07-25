package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.ZuulRequestErrorReason.ClientLimitReached
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{ClientUpdated, ClientDeregistered}
import com.blinkbox.books.auth.server.services.DefaultClientService
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.testkit.{PublisherSpy, TestH2}
import com.blinkbox.books.time.StoppedClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class DefaultClientServiceSpecs extends FlatSpec with Matchers with ScalaFutures {

  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val cl = StoppedClock()
  implicit override val patienceConfig = PatienceConfig(timeout = Span(500, Millis), interval = Span(20, Millis))

  val tables = TestH2.tables

  import tables._
  import driver.simple._

  trait TestEnv {
    val db = TestH2.db
    val publisherSpy = new PublisherSpy
    val authRepository = new DefaultAuthRepository(tables)
    val clientRepository = new DefaultClientRepository(tables)
    val clientService = new DefaultClientService(TestH2.db, clientRepository, authRepository, publisherSpy)

    val userIdA = UserId(1)
    val userIdB = UserId(2)
    val userIdC = UserId(3)

    val userA = User(userIdA, cl.now(), cl.now(), "user.a@test.tst", "A First", "A Last", "a-password", true)
    val userB = User(userIdB, cl.now(), cl.now(), "user.b@test.tst", "B First", "B Last", "b-password", true)
    val userC = User(userIdC, cl.now(), cl.now(), "user.c@test.tst", "C First", "C Last", "c-password", true)

    val authenticatedUserA = AuthenticatedUser(userIdA.value, None, Map.empty)
    val authenticatedUserB = AuthenticatedUser(userIdB.value, None, Map.empty)
    val authenticatedUserC = AuthenticatedUser(userIdC.value, None, Map.empty)

    val clientIdA1 = ClientId(1)
    val clientIdA2 = ClientId(2)
    val clientIdA3 = ClientId(3)

    val clientA1 = Client(clientIdA1, cl.now(), cl.now(), userIdA, "Client A1", "Test brand A1", "Test model A1", "Test OS A1", "test-secret-a1", false)
    val clientA2 = Client(clientIdA2, cl.now(), cl.now(), userIdA, "Client A2", "Test brand A2", "Test model A2", "Test OS A2", "test-secret-a2", false)
    val clientA3 = Client(clientIdA3, cl.now(), cl.now(), userIdA, "Client A3", "Test brand A3", "Test model A3", "Test OS A3", "test-secret-a3", true)

    val clientsC =
      for (id <- 4 until 16)
      yield Client(ClientId(id), cl.now(), cl.now(), userIdC, s"Client C$id", s"Test brand C$id", s"Test model C$id", s"Test OS C$id", s"test-secret-c$id", false)

    val clientInfoA1 = ClientInfo(s"urn:blinkbox:zuul:client:1", "/clients/1", "Client A1", "Test brand A1", "Test model A1", "Test OS A1", None, cl.now())
    val clientInfoA2 = ClientInfo(s"urn:blinkbox:zuul:client:2", "/clients/2", "Client A2", "Test brand A2", "Test model A2", "Test OS A2", None, cl.now())

    val fullClientPatch = ClientPatch(Some("Patched name"), Some("Patched brand"), Some("Patched model"), Some("Patched OS"))
    val fullPatchedClientA1 = clientA1.copy(name = "Patched name", brand = "Patched brand", model = "Patched model", os = "Patched OS")
    val fullPatchedClientInfoA1 = clientInfoA1.copy(client_name = "Patched name", client_brand = "Patched brand", client_model = "Patched model", client_os = "Patched OS")

    val forbiddenAccesses = ((clientIdA3, authenticatedUserA) ::
      (clientIdA1, authenticatedUserB) ::
      (clientIdA2, authenticatedUserB) ::
      (clientIdA3, authenticatedUserB) ::
      (ClientId(100), authenticatedUserA) ::
      (ClientId(100), authenticatedUserB) ::Nil)

    val clientRegistration = ClientRegistration("Test name", "Test brand", "Test model", "Test OS")

    db.withSession { implicit session =>
      tables.users ++= Seq(userA, userB)
      tables.clients ++= Seq(clientA1, clientA2, clientA3) ++ clientsC
    }
  }

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

    forbiddenAccesses foreach { case (c, u) =>
      whenReady(clientService.getClientById(c)(u)) { infoOpt =>
        infoOpt shouldBe empty
      }
    }
  }

  it should "delete clients by id given they are owned by the user and not already de-registered" in new TestEnv {
    whenReady(clientService.deleteClient(clientIdA1)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA1) }
      publisherSpy.events should equal(ClientDeregistered(clientA1.copy(isDeregistered = true)) :: Nil)
    }

    whenReady(clientService.deleteClient(clientIdA2)(authenticatedUserA)) { infoOpt =>
      infoOpt shouldBe defined
      infoOpt.foreach { _ should equal(clientInfoA2) }
      publisherSpy.events should equal(
        ClientDeregistered(clientA2.copy(isDeregistered = true)) ::
        ClientDeregistered(clientA1.copy(isDeregistered = true)) ::Nil)
    }
  }

  it should "not allow deletion of non-owned, non-existing or already-deregistered clients" in new TestEnv {
    forbiddenAccesses foreach { case (c, u) =>
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
    forbiddenAccesses foreach { case (c, u) =>
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
        s"urn:blinkbox:zuul:client:$id", s"/clients/$id", "Test name", "Test brand", "Test model", "Test OS", Some(secret), cl.now()))
    }

  }

  it should "respect client limits for an user preventing the creation of more clients" in new TestEnv {
    clientService.registerClient(clientRegistration)(authenticatedUserC) onComplete {
      case Success(_) => fail("Client registration should not be allowed if user has already reached limit")
      case Failure(ex) =>
        ex should equal(ZuulRequestException("Max clients ($MaxClients) already registered", InvalidRequest, Some(ClientLimitReached)))

        db.withSession { implicit session =>
          tables.clients.where(_.id === ClientId(4)).map(_.isDeregistered).update(true)
        }

        clientService.registerClient(clientRegistration)(authenticatedUserC) onComplete {
          case Success(_) => // Do nothing
          case Failure(ex) => fail("Client registration should be allowed if user has de-registerd a client")
        }
    }
  }
}

