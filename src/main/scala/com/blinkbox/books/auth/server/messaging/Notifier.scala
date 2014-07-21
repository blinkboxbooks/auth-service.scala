package com.blinkbox.books.auth.server.messaging

import com.blinkbox.books.auth.server.data.{Client, User}
import com.blinkbox.books.time.{Clock, TimeSupport}
import scala.concurrent.{ExecutionContext, Future}

trait Notifier {
  this: TimeSupport =>
  def userRegistered(user: User, client: Option[Client]): Future[Unit]
  def userUpdated(oldUser: User, newUser: User): Future[Unit]
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit]
  def clientRegistered(client: Client): Future[Unit]
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit]
  def clientDeregistered(client: Client): Future[Unit]
}

class MultiNotifier(notifiers: Notifier*)(implicit executionContext: ExecutionContext, val clock: Clock) extends Notifier with TimeSupport {
  def userRegistered(user: User, client: Option[Client]): Future[Unit] = Future {
    notifiers.foreach(_.userRegistered(user, client))
  }
  def userUpdated(oldUser: User, newUser: User): Future[Unit] = Future {
    notifiers.foreach(_.userUpdated(oldUser, newUser))
  }
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit] = Future {
    notifiers.foreach(_.userAuthenticated(user, client))
  }
  def clientRegistered(client: Client): Future[Unit] = Future {
    notifiers.foreach(_.clientRegistered(client))
  }
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit] = Future {
    notifiers.foreach(_.clientUpdated(oldClient, newClient))
  }
  def clientDeregistered(client: Client): Future[Unit] = Future {
    notifiers.foreach(_.clientDeregistered(client))
  }
}

class LegacyRabbitMqNotifier(implicit executionContext: ExecutionContext, val clock: Clock) extends Notifier with TimeSupport {
  def userRegistered(user: User, client: Option[Client]): Future[Unit] = Future {}
  def userUpdated(oldUser: User, newUser: User): Future[Unit] = Future {}
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit] = Future {}
  def clientRegistered(client: Client): Future[Unit] = Future {}
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit] = Future {}
  def clientDeregistered(client: Client): Future[Unit] = Future {}
}

class RabbitMqNotifier(implicit executionContext: ExecutionContext, val clock: Clock) extends Notifier with TimeSupport {
  def userRegistered(user: User, client: Option[Client]): Future[Unit] = Future {}
  def userUpdated(oldUser: User, newUser: User): Future[Unit] = Future {}
  def userAuthenticated(user: User, client: Option[Client]): Future[Unit] = Future {}
  def clientRegistered(client: Client): Future[Unit] = Future {}
  def clientUpdated(oldClient: Client, newClient: Client): Future[Unit] = Future {}
  def clientDeregistered(client: Client): Future[Unit] = Future {}
}

