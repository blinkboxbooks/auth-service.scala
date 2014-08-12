package com.blinkbox.books.auth.server.services

import java.sql.DataTruncation

import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidRequest
import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.events.{ClientRegistered, ClientUpdated, Publisher}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
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
    (db: Database, clientRepo: ClientRepository[Profile], authRepo: AuthRepository[Profile], userRepo: UserRepository[Profile], events: Publisher)
    (implicit executionContext: ExecutionContext, clock: Clock) extends ClientService with ClientInfoFactory {

  // TODO: Make this configurable
  val MaxClients = 12

  implicit def userId(implicit user: AuthenticatedUser): UserId = UserId(user.id)

  override def registerClient(registration: ClientRegistration)(implicit user: AuthenticatedUser) = Future {
    val (usr, client) = db.withTransaction { implicit transaction =>
      if (clientRepo.activeClientCount(userId) >= MaxClients) {
        throw Failures.clientLimitReached
      }
      val u = userRepo.userWithId(userId).getOrElse(throw Failures.unverifiedIdentity)
      val c = clientRepo.createClient(userId, registration)
      (u, c)
    }
    events.publish(ClientRegistered(usr, client))
    clientInfo(client, includeClientSecret = true)
  } transform(identity, {
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
    val client = db.withTransaction { implicit transaction =>
      val u = userRepo.userWithId(userId).getOrElse(throw Failures.unverifiedIdentity)
      for {
        oc <- clientRepo.clientWithId(u.id, id)
        nc =  oc.copy(updatedAt = clock.now(),
                      name = patch.client_name.getOrElse(oc.name),
                      brand = patch.client_brand.getOrElse(oc.brand),
                      model = patch.client_model.getOrElse(oc.model),
                      os = patch.client_os.getOrElse(oc.os))
        _  =  clientRepo.updateClient(nc.userId, nc)
        _  =  events.publish(ClientUpdated(u, oc, nc))
      } yield nc
    }
    client.map(clientInfo(_))
  }

  override def deleteClient(id: ClientId)(implicit user: AuthenticatedUser): Future[Option[ClientInfo]] = Future {
    val client = db.withTransaction { implicit transaction =>
      val u = userRepo.userWithId(userId).getOrElse(throw Failures.unverifiedIdentity)
      for {
        c <- clientRepo.clientWithId(u.id, id).map(_.copy(updatedAt = clock.now(), isDeregistered = true))
        _ =  clientRepo.updateClient(c.userId, c) // TODO: Could remove userId from this method signature
        _ =  authRepo.refreshTokensByClientId(c.id).foreach(authRepo.revokeRefreshToken)
        _ =  events.publish(ClientRegistered(u, c))
      } yield c
    }
    client.map(clientInfo(_))
  }


}
