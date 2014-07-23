package com.blinkbox.books.auth.server.events

import java.net.URL
import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data.{Client, User}
import com.blinkbox.books.rabbitmq.{RabbitMq, RabbitMqConfig}
import com.blinkbox.books.time.Clock
import com.rabbitmq.client.AMQP
import org.slf4j.{LoggerFactory, MDC}

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

class LegacyRabbitMqPublisher(config: RabbitMqConfig)(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher {
  val log = LoggerFactory.getLogger(classOf[Publisher])
  val channel = RabbitMq.reliableConnection(config).createChannel()
  channel.exchangeDeclare("Events", "topic", true)

  def publish(event: Event): Future[Unit] = Future {
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
      Try(channel.basicPublish("Events", key, MessageProperties.PersistentXml, bytes)).recover {
        case NonFatal(e) =>
          // TODO: Factor this MDC swizzling out into a withMdc function in a shared lib
          val originalMDC = MDC.getCopyOfContextMap
          MDC.put("amqpExchange", "Events")
          MDC.put("amqpRoutingKey", key)
          MDC.put("amqpMessageBody", message.toString())
          log.error("Failed to publish AMQP message", e)
          if (originalMDC == null) MDC.clear() else MDC.setContextMap(originalMDC)
      }
    }
  }

  private def routingKey(message: Elem): String = new URL(message.namespace).getPath.substring(1).replace('/', '.') + "." + message.label
}

// TODO: Implement this
class RabbitMqPublisher(implicit executionContext: ExecutionContext, clock: Clock) extends Publisher {
  def publish(event: Event): Future[Unit] = Future {}
}

