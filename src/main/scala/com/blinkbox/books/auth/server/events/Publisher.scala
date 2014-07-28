package com.blinkbox.books.auth.server.events

import java.net.URL
import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data.{Client, User}
import com.blinkbox.books.time.Clock
import com.blinkbox.books.logging._
import com.rabbitmq.client.{AMQP, Channel}
import com.typesafe.scalalogging.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.Elem

sealed trait Event
case class UserRegistered(user: User, client: Option[Client]) extends Event
case class UserUpdated(oldUser: User, newUser: User) extends Event
case class UserAuthenticated(user: User, client: Option[Client]) extends Event
case class ClientRegistered(client: Client) extends Event
case class ClientUpdated(oldClient: Client, newClient: Client) extends Event
case class ClientDeregistered(client: Client) extends Event

trait Publisher {
  def publish(event: Event): Future[Unit]
}

object Publisher {
  implicit class PublisherComposition(publisher: Publisher) {
    def ~(other: Publisher)(implicit executionContext: ExecutionContext): Publisher = (publisher, other) match {
      case (mp: MultiPublisher, p) => new MultiPublisher(p :: mp.publishers)
      case (p, mp: MultiPublisher) => new MultiPublisher(p :: mp.publishers)
      case (p1, p2) => new MultiPublisher(p1 :: p2 :: Nil)
    }
  }

  private class MultiPublisher(val publishers: List[Publisher])(implicit executionContext: ExecutionContext) extends Publisher {
    def publish(event: Event): Future[Unit] = Future { publishers.foreach(_.publish(event)) }
  }
}

private object MessageProperties {
  val PersistentDeliveryMode = 2
  val PersistentXml = new AMQP.BasicProperties("application/xml", null, null, PersistentDeliveryMode, 0, null, null, null, null, null, null, null, null, null)
}

class LegacyRabbitMqPublisher(channel: Channel)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher with Logging {
  val ExchangeName = "Events"
  val ExchangeType = "topic"
  val IsDurable = true

  channel.exchangeDeclare(ExchangeName, ExchangeType, IsDurable)

  override def publish(event: Event): Future[Unit] = Future {
    val messages = event match {
      case UserRegistered(user, client) => (Some(XmlMessages.userRegistered(user)) :: client.map(XmlMessages.clientRegistered) :: Nil).flatten
      case UserUpdated(oldUser, newUser) => XmlMessages.userUpdated(oldUser, newUser) :: Nil
      case UserAuthenticated(user, client) => XmlMessages.userAuthenticated(user, client) :: Nil
      case ClientRegistered(client) => XmlMessages.clientRegistered(client) :: Nil
      case ClientUpdated(oldClient, newClient) => XmlMessages.clientUpdated(oldClient, newClient) :: Nil
      case ClientDeregistered(client) => XmlMessages.clientDeregistered(client) :: Nil
    }
    messages.foreach { message =>
      val bytes = message.toString().getBytes(StandardCharsets.UTF_8)
      val key = routingKey(message)
      Try(channel.basicPublish(ExchangeName, key, MessageProperties.PersistentXml, bytes)).recover {
        case NonFatal(e) => logger.withContext("rabbitMqExchange" -> ExchangeName, "rabbitMqRoutingKey" -> key, "rabbitMqMessageBody" -> message) {
          _.error("Failed to publish RabbitMQ message", e)
        }
      }
    }
  }

  private def routingKey(message: Elem): String = new URL(message.namespace).getPath.substring(1).replace('/', '.') + "." + message.label
}

// TODO: Implement this when we've decided on the new exchange structure and message format
class RabbitMqPublisher(channel: Channel)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher {
  override def publish(event: Event): Future[Unit] = Future {}
}

