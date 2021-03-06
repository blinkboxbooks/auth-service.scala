package com.blinkbox.books.auth.server.data

import java.security.SecureRandom

import com.blinkbox.books.auth.server.ClientRegistration
import com.blinkbox.books.slick.{TablesSupport, SlickTypes}
import com.blinkbox.books.time.{Clock, TimeSupport}
import com.blinkbox.security.jwt.util.Base64

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait ClientRepository[Profile <: BasicProfile] extends SlickTypes[Profile] {
  def activeClients(userId: UserId)(implicit session: Session): List[Client]
  def activeClientCount(userId: UserId)(implicit session: Session): Int
  def clientWithId(userId: UserId, id: ClientId)(implicit session: Session): Option[Client]
  def updateClient(userId: UserId, client: Client)(implicit session: Session): Unit
  def createClient(userId: UserId, registration: ClientRegistration)(implicit session: Session): Client
}

trait JdbcClientRepository[Profile <: JdbcProfile] extends ClientRepository[Profile] with TablesSupport[Profile, ZuulTables[Profile]] {
  this: TimeSupport =>

  import tables._
  import driver.simple._

  override def activeClients(userId: UserId)(implicit session: Session): List[Client] =
    clients.filter(c => c.userId === userId && !c.isDeregistered).list

  override def activeClientCount(userId: UserId)(implicit session: Session): Int =
    clients.filter(c => c.userId === userId && !c.isDeregistered).length.run

  override def updateClient(userId: UserId, client: Client)(implicit session: Session): Unit =
    clients.filter(c => c.id === client.id && c.userId === userId).update(client)

  override def clientWithId(userId: UserId, id: ClientId)(implicit session: Session): Option[Client] =
    clients.filter(c => c.id === id && c.userId === userId && !c.isDeregistered).firstOption

  override def createClient(userId: UserId, registration: ClientRegistration)(implicit session: Session): Client = {
    val client = newClient(userId, registration)
    val id = (clients returning clients.map(_.id)) += client
    client.copy(id = id)
  }

  private def newClient(userId: UserId, r: ClientRegistration) = {
    val now = clock.now()
    val buf = new Array[Byte](32)
    new SecureRandom().nextBytes(buf)
    val secret = Base64.encode(buf)
    Client(ClientId.Invalid, now, now, userId, r.name, r.brand, r.model, r.os, secret, false)
  }
}

class DefaultClientRepository[Profile <: JdbcProfile](val tables: ZuulTables[Profile])(implicit val clock: Clock)
  extends TimeSupport with JdbcClientRepository[Profile]
