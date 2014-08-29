package com.blinkbox.books.auth.server.events

import java.net.URL
import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data
import com.blinkbox.books.logging._
import com.blinkbox.books.messaging.{EventBody, JsonEventBody}
import com.blinkbox.books.schemas.events.client.v2.{Client, ClientId}
import com.blinkbox.books.schemas.events.user.v2._
import com.blinkbox.books.time.Clock
import com.rabbitmq.client.{AMQP, Channel}
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.Elem

sealed trait Event
case class UserRegistered(user: data.User) extends Event
case class UserUpdated(oldUser: data.User, newUser: data.User) extends Event
case class UserAuthenticated(user: data.User, client: Option[data.Client]) extends Event
case class UserPasswordChanged(user: data.User) extends Event
case class UserPasswordResetRequested(user: data.User, token: String, link: URL) extends Event
case class ClientRegistered(user: data.User, client: data.Client) extends Event
case class ClientUpdated(user: data.User, oldClient: data.Client, newClient: data.Client) extends Event
case class ClientDeregistered(user: data.User, client: data.Client) extends Event

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

class LegacyRabbitMqPublisher(channel: Channel)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher with StrictLogging {
  private val EventsExchangeName = "Events"
  private val EventsExchangeType = "topic"
  private val EmailExchangeName = "Emails.Outbound"
  private val EmailExchangeType = "fanout"
  private val IsDurable = true

  channel.exchangeDeclare(EventsExchangeName, EventsExchangeType, IsDurable)
  channel.exchangeDeclare(EmailExchangeName, EmailExchangeType, IsDurable)

  override def publish(event: Event): Future[Unit] = Future {
    publishEvent(event)
    sendEmail(event)
  }

  private def publishEvent(event: Event): Future[Unit] = Future {
    eventBody(event) foreach { body =>
      val bytes = body.toString().getBytes(StandardCharsets.UTF_8)
      val key = routingKey(body)
      Try(channel.basicPublish(EventsExchangeName, key, MessageProperties.PersistentXml, bytes)).recover {
        case NonFatal(e) => logger.withContext("rabbitMqExchange" -> EventsExchangeName, "rabbitMqRoutingKey" -> key, "rabbitMqMessageBody" -> body) {
          _.error("Failed to publish RabbitMQ message", e)
        }
      }
    }
  }

  private def eventBody(event: Event): Option[Elem] = event match {
    case UserRegistered(user) => Some(XmlMessages.userRegistered(user))
    case UserUpdated(oldUser, newUser) => Some(XmlMessages.userUpdated(oldUser, newUser))
    case UserAuthenticated(user, client) => Some(XmlMessages.userAuthenticated(user, client))
    case ClientRegistered(_, client) => Some(XmlMessages.clientRegistered(client))
    case ClientUpdated(_, oldClient, newClient) => Some(XmlMessages.clientUpdated(oldClient, newClient))
    case ClientDeregistered(_, client) => Some(XmlMessages.clientDeregistered(client))
    case _ => None
  }

  private def routingKey(message: Elem): String = new URL(message.namespace).getPath.substring(1).replace('/', '.') + "." + message.label

  private def sendEmail(event: Event): Future[Unit] = Future {
    emailBody(event) foreach { body =>
      val bytes = body.toString().getBytes(StandardCharsets.UTF_8)
      Try(channel.basicPublish(EmailExchangeName, "", MessageProperties.PersistentXml, bytes)).recover {
        case NonFatal(e) => logger.withContext("rabbitMqExchange" -> EventsExchangeName, "rabbitMqMessageBody" -> body) {
          _.error("Failed to publish RabbitMQ message", e)
        }
      }
    }
  }

  private def emailBody(event: Event): Option[Elem] = event match {
    case UserRegistered(user) =>
      Some(XmlMessages.sendEmail(user, "welcome", Map("salutation" -> user.firstName)))
    case UserPasswordChanged(user) =>
      Some(XmlMessages.sendEmail(user, "password_confirmed", Map("salutation" -> user.firstName)))
    case UserPasswordResetRequested(user, token, link) =>
      Some(XmlMessages.sendEmail(user, "password_reset", Map("salutation" -> user.firstName, "resetLink" -> link, "resetToken" -> token)))
    case _ =>
      None
  }
}

class RabbitMqPublisher(channel: Channel)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher with StrictLogging {
  private val ExchangeName = "Agora" // TODO: Make this configurable
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

  private implicit def clientId2eventClientId(id: data.ClientId) = ClientId(id.value)
  private implicit def userId2eventUserId(id: data.UserId) = UserId(id.value)
  private implicit def client2eventClient(c: data.Client) = Client(c.id, c.name, c.brand, c.model, c.os)
  private implicit def user2eventUser(u: data.User) = User(u.id, u.username, u.firstName, u.lastName)
  private implicit def user2eventUserProfile(u: data.User) = UserProfile(u, AccountInfo("1.0", ssoUserId = u.ssoId), MarketingPreferences(u.allowMarketing))

  private def eventBody(event: Event): EventBody = event match {
    case ClientDeregistered(user, client) => JsonEventBody(Client.Deregistered(client.updatedAt, user, client))
    case ClientRegistered(user, client) => JsonEventBody(Client.Registered(client.createdAt, user, client))
    case ClientUpdated(user, oldClient, newClient) => JsonEventBody(Client.Updated(newClient.updatedAt, user, newClient, oldClient))
    case UserAuthenticated(user, client) => JsonEventBody(User.Authenticated(clock.now(), user, client.map(client2eventClient)))
    case UserPasswordChanged(user) => JsonEventBody(User.PasswordChanged(user.updatedAt, user))
    case UserPasswordResetRequested(user, token, link) => JsonEventBody(User.PasswordResetRequested(clock.now(), user.username, token, link))
    case UserRegistered(user) => JsonEventBody(User.Registered(user.createdAt, user))
    case UserUpdated(oldUser, newUser) => JsonEventBody(User.Updated(newUser.updatedAt, newUser, oldUser))
  }

  private def basicProperties(body: EventBody) = new AMQP.BasicProperties(
    body.contentType.mediaType.toString(),
    body.contentType.charset.getOrElse(StandardCharsets.UTF_8).toString,
    null,
    MessageProperties.PersistentDeliveryMode,
    0, null, null, null, null, null, null, null, null, null)
}

