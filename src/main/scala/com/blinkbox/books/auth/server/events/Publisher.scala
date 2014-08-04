package com.blinkbox.books.auth.server.events

import java.net.URL
import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data
import com.blinkbox.books.logging._
import com.blinkbox.books.messaging.{EventBody, JsonEventBody}
import com.blinkbox.books.schemas.events.client.v2.{Client, ClientId}
import com.blinkbox.books.schemas.events.user.v2.{User, UserId}
import com.blinkbox.books.time.Clock
import com.rabbitmq.client.{AMQP, Channel}
import com.typesafe.scalalogging.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.Elem

sealed trait Event
case class UserRegistered(user: data.User) extends Event
case class UserUpdated(oldUser: data.User, newUser: data.User) extends Event
case class UserAuthenticated(user: data.User, client: Option[data.Client]) extends Event
case class ClientRegistered(client: data.Client) extends Event
case class ClientUpdated(oldClient: data.Client, newClient: data.Client) extends Event
case class ClientDeregistered(client: data.Client) extends Event

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
  private val ExchangeName = "Events"
  private val ExchangeType = "topic"
  private val IsDurable = true

  channel.exchangeDeclare(ExchangeName, ExchangeType, IsDurable)

  override def publish(event: Event): Future[Unit] = Future {
    val body = eventBody(event)
    val bytes = body.toString().getBytes(StandardCharsets.UTF_8)
    val key = routingKey(body)
    Try(channel.basicPublish(ExchangeName, key, MessageProperties.PersistentXml, bytes)).recover {
      case NonFatal(e) => logger.withContext("rabbitMqExchange" -> ExchangeName, "rabbitMqRoutingKey" -> key, "rabbitMqMessageBody" -> body) {
        _.error("Failed to publish RabbitMQ message", e)
      }
    }
  }

  def eventBody(event: Event): Elem = event match {
    case UserRegistered(user) => XmlMessages.userRegistered(user)
    case UserUpdated(oldUser, newUser) => XmlMessages.userUpdated(oldUser, newUser)
    case UserAuthenticated(user, client) => XmlMessages.userAuthenticated(user, client)
    case ClientRegistered(client) => XmlMessages.clientRegistered(client)
    case ClientUpdated(oldClient, newClient) => XmlMessages.clientUpdated(oldClient, newClient)
    case ClientDeregistered(client) => XmlMessages.clientDeregistered(client)
  }

  private def routingKey(message: Elem): String = new URL(message.namespace).getPath.substring(1).replace('/', '.') + "." + message.label
}

class RabbitMqPublisher(channel: Channel)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher with Logging {
  private val ExchangeName = "Shop"
  private val ExchangeType = "headers"
  private val IsDurable = true

  channel.exchangeDeclare(ExchangeName, ExchangeType, IsDurable)

  override def publish(event: Event): Future[Unit] = Future {
    val body = eventBody(event)
    Try(channel.basicPublish(ExchangeName, "", basicProperties(body), body.content)).recover {
      case NonFatal(e) => logger.withContext("rabbitMqExchange" -> ExchangeName, "rabbitMqMessageBody" -> body.asString()) {
        _.error("Failed to publish RabbitMQ message", e)
      }
    }
  }

  import scala.language.implicitConversions

  private implicit def clientId2event(id: data.ClientId) = ClientId(id.value)
  private implicit def userId2event(id: data.UserId) = UserId(id.value)
  private implicit def client2event(c: data.Client) = Client(ClientId(c.id.value), c.name, c.brand, c.model, c.os)
  private implicit def user2event(u: data.User) = User(UserId(u.id.value), u.username, u.firstName, u.lastName, u.allowMarketing)

  private def eventBody(event: Event): EventBody = event match {
    case UserRegistered(user) => JsonEventBody(User.Registered(user.createdAt, user))
    case UserUpdated(oldUser, newUser) => JsonEventBody(User.Updated(newUser.updatedAt, newUser, oldUser))
    case UserAuthenticated(user, client) => JsonEventBody(User.Authenticated(clock.now(), user, client.map(client2event)))
    case ClientRegistered(client) => JsonEventBody(Client.Registered(client.createdAt, client.userId, client))
    case ClientUpdated(oldClient, newClient) => JsonEventBody(Client.Updated(newClient.updatedAt, newClient.userId, newClient, oldClient))
    case ClientDeregistered(client) => JsonEventBody(Client.Deregistered(client.updatedAt, client.userId, client))
  }

  private def basicProperties(body: EventBody) = new AMQP.BasicProperties(
    body.contentType.mediaType.toString(),
    body.contentType.charset.getOrElse(StandardCharsets.UTF_8).toString,
    null,
    MessageProperties.PersistentDeliveryMode,
    0, null, null, null, null, null, null, null, null, null)
}

