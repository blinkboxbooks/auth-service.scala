package com.blinkbox.books.auth.server.events

import com.blinkbox.books.auth.server.data.{Client, User}
import com.blinkbox.books.time.Clock

import scala.concurrent.{ExecutionContext, Future}

trait Publisher {
  def userRegistered(user: User, client: Option[Client]): Future[Unit]
  def userUpdated(oldUser: User, newUser: User): Future[Unit]
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit]
  def clientRegistered(client: Client): Future[Unit]
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit]
  def clientDeregistered(client: Client): Future[Unit]
}

object Publisher {
  implicit class PublisherComposition(publisher: Publisher) {
    def ~(other: Publisher)(implicit executionContext: ExecutionContext): Publisher = (publisher, other) match {
      case (mp: MultiPublisher, p) => new MultiPublisher(p :: mp.publishers)
      case (p, mp: MultiPublisher) => new MultiPublisher(p :: mp.publishers)
      case (p1, p2) => new MultiPublisher(p1 :: p2 :: Nil)
    }
  }
}

private sealed class MultiPublisher(val publishers: List[Publisher])(implicit executionContext: ExecutionContext) extends Publisher {
  def userRegistered(user: User, client: Option[Client]) = Future { publishers.foreach(_.userRegistered(user, client)) }
  def userUpdated(oldUser: User, newUser: User) = Future { publishers.foreach(_.userUpdated(oldUser, newUser)) }
  def userAuthenticated(user: User, client: Option[Client]) = Future { publishers.foreach(_.userAuthenticated(user, client)) }
  def clientRegistered(client: Client) = Future { publishers.foreach(_.clientRegistered(client)) }
  def clientUpdated(oldClient: Client, newClient: Client) = Future { publishers.foreach(_.clientUpdated(oldClient, newClient)) }
  def clientDeregistered(client: Client) = Future { publishers.foreach(_.clientDeregistered(client)) }
}

// TODO: Implement this
class LegacyRabbitMqPublisher(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher {
  def userRegistered(user: User, client: Option[Client]): Future[Unit] = Future {}
  def userUpdated(oldUser: User, newUser: User): Future[Unit] = Future {}
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit] = Future {}
  def clientRegistered(client: Client): Future[Unit] = Future {}
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit] = Future {}
  def clientDeregistered(client: Client): Future[Unit] = Future {}
}

// TODO: Implement this
class RabbitMqPublisher(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher {
  def userRegistered(user: User, client: Option[Client]): Future[Unit] = Future {}
  def userUpdated(oldUser: User, newUser: User): Future[Unit] = Future {}
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit] = Future {}
  def clientRegistered(client: Client): Future[Unit] = Future {}
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit] = Future {}
  def clientDeregistered(client: Client): Future[Unit] = Future {}
}

