package com.blinkbox.books.auth.server.services

import java.sql.DataTruncation

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server.events.{ClientDeregistered, ClientUpdated, ClientRegistered, Publisher}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.auth.server.data.{AuthRepository, UserId, ClientRepository, ClientId}
import com.blinkbox.books.auth.server._
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.profile.BasicProfile

trait ClientService {
  def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser): Future[ClientInfo]
  def listClients()(implicit user: AuthenticatedUser): Future[ClientList]
  def getClientById(id: ClientId)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def updateClient(id: ClientId, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
  def deleteClient(id: ClientId)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]]
}

class DefaultClientService[Profile <: BasicProfile, Database <: Profile#Backend#Database]
    (db: Database, clientRepo: ClientRepository[Profile], authRepo: AuthRepository[Profile], events: Publisher)
    (implicit executionContext: ExecutionContext, clock: Clock) extends ClientService with ClientInfoFactory {

  // TODO: Make this configurable
  val MaxClients = 12

  implicit def userId(implicit user: AuthenticatedUser): UserId = UserId(user.id)

  override def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser) = Future {
    val client = db.withTransaction { implicit transaction =>
      if (clientRepo.activeClientCount(userId) >= MaxClients) {
        FailWith.clientLimitReached
      }
      clientRepo.createClient(userId, registration)
    }

    events.publish(ClientRegistered(client))

    clientInfo(client, includeClientSecret = true)

  } transform(identity, _ match {
    case e: DataTruncation => ZuulRequestException(e.getMessage, InvalidRequest)
    case e => e
  })

  override def getClientById(id: ClientId)(implicit user: AuthenticatedUser) = Future {
    val client = db.withSession(implicit session => clientRepo.clientWithId(userId, id))
    client.map(clientInfo(_))
  }

  override def listClients()(implicit user: AuthenticatedUser)= Future {
    val clientList = db.withSession(implicit session => clientRepo.activeClients(userId))
    ClientList(clientList.map(clientInfo(_)))
  }

  override def updateClient(id: ClientId, patch: ClientPatch)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val clients = db.withTransaction { implicit transaction =>
      val clientPair = clientRepo.clientWithId(userId, id) map { oldClient =>
        val newClient = oldClient.copy(
          updatedAt = clock.now(),
          name = patch.client_name.getOrElse(oldClient.name),
          brand = patch.client_brand.getOrElse(oldClient.brand),
          model = patch.client_model.getOrElse(oldClient.model),
          os = patch.client_os.getOrElse(oldClient.os))
        (oldClient, newClient)
      }
      clientPair foreach { case (_, c) => clientRepo.updateClient(userId, c) }
      clientPair
    }

    clients foreach { case (o, n) => events.publish(ClientUpdated(o, n)) }
    clients map { case (_, c) => clientInfo(c) }
  }

  override def deleteClient(id: ClientId)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val client = db.withSession { implicit session =>
      val newClient = clientRepo.clientWithId(userId, id).map(_.copy(updatedAt = clock.now(), isDeregistered = true))
      newClient.foreach { c =>
        clientRepo.updateClient(userId, c)
        authRepo.refreshTokensByClientId(c.id).foreach(authRepo.revokeRefreshToken)
      }
      newClient
    }
    client.foreach(c => events.publish(ClientDeregistered(c)))
    client.map(clientInfo(_))
  }
}
