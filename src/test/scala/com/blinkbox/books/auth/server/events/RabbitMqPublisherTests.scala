package com.blinkbox.books.auth.server.events

import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data
import com.blinkbox.books.schemas.events.client.v2.{ClientId, Client}
import com.blinkbox.books.schemas.events.user.v2.{User, UserId}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.StoppedClock
import com.rabbitmq.client.{AMQP, Channel}
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.jackson.Serialization
import org.json4s.{CustomSerializer, DefaultFormats}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}

import scala.concurrent.ExecutionContext.Implicits.global

object ISODateTimeSerializer extends CustomSerializer[DateTime](_ => ({
  case JString(s) => ISODateTimeFormat.dateTime.parseDateTime(s).toDateTime(DateTimeZone.UTC)
  case JNull => null
}, {
  case d: DateTime => JString(ISODateTimeFormat.dateTime.print(d))
}))

class RabbitMqPublisherTests extends FunSuite with ScalaFutures with MockitoSyrup {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))
  implicit val clock = StoppedClock()
  implicit val formats = DefaultFormats + ISODateTimeSerializer
  
  val userData = data.User(data.UserId(123), clock.now().minusDays(30), clock.now().minusDays(15), "john@example.org", "John", "Doe", "hash", allowMarketing = true)
  val clientData = data.Client(data.ClientId(456), clock.now().minusDays(29), clock.now().minusDays(14), userData.id, "Test Client", "Apple", "iPhone", "iOS", "secret", isDeregistered = false)

  val eventUser =  User(UserId(userData.id.value), userData.username, userData.firstName, userData.lastName, userData.allowMarketing)
  val eventClient = Client(ClientId(clientData.id.value), clientData.name, clientData.brand, clientData.model, clientData.os)

  test("Declares an exchange with the correct parameters on instantiation") {
    val channel = mock[Channel]
    new RabbitMqPublisher(channel)
    verify(channel).exchangeDeclare("Shop", "headers", true)
  }

  test("Sends client deregistered messages with the correct media type and payload") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(ClientDeregistered(clientData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.client.deregistered.v2+json"),
      messageOfType[Client.Registered] { msg => msg.timestamp == clientData.updatedAt && msg.client == eventClient })
    }
  }

  test("Sends client registered messages with the correct media type and payload") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(ClientRegistered(clientData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.client.registered.v2+json"),
      messageOfType[Client.Registered] { msg => msg.timestamp == clientData.createdAt && msg.client == eventClient })
    }
  }

  test("Sends client updated messages with the correct media type and payload") {
    val newClientData = clientData.copy(updatedAt = clock.now().minusSeconds(1), name = "New Client")
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(ClientUpdated(clientData, newClientData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.client.updated.v2+json"),
      messageOfType[Client.Updated] { msg =>
        msg.timestamp == newClientData.updatedAt &&
        msg.client == Client(ClientId(newClientData.id.value), newClientData.name, newClientData.brand, newClientData.model, newClientData.os) &&
        msg.previousDetails == eventClient })
    }
  }

  test("Sends user authenticated messages with the correct media type and payload, when there is no client") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(UserAuthenticated(userData, None))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.authenticated.v2+json"),
      messageOfType[User.Authenticated] { msg => msg.timestamp == clock.now() && msg.user == eventUser && msg.client == None })
    }
  }

  test("Sends user authenticated messages with the correct media type and payload, when there is a client") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(UserAuthenticated(userData, Some(clientData)))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.authenticated.v2+json"),
      messageOfType[User.Authenticated] { msg => msg.timestamp == clock.now() && msg.user == eventUser && msg.client == Some(eventClient) })
    }
  }

  test("Sends user registered messages with the correct media type and payload") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(UserRegistered(userData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.registered.v2+json"),
      messageOfType[User.Registered] { msg => msg.timestamp == userData.createdAt && msg.user == eventUser })
    }
  }

  test("Sends user updated messages with the correct media type and payload") {
    val newUserData = userData.copy(updatedAt = clock.now().minusSeconds(1), username = "fred@example.org", firstName = "Fred", lastName = "Bloggs")
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(UserUpdated(userData, newUserData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.updated.v2+json"),
      messageOfType[User.Updated] { msg =>
        msg.timestamp == newUserData.updatedAt &&
        msg.user == User(UserId(newUserData.id.value), newUserData.username, newUserData.firstName, newUserData.lastName, newUserData.allowMarketing) &&
        msg.previousDetails == eventUser })
    }
  }

  private def shopExchange = Matchers.eq("Shop")

  private def noRoutingKey = Matchers.eq("")

  private def propertiesFor(mediaType: String) = argThat { props: AMQP.BasicProperties =>
    props.getContentType == mediaType &&
    props.getContentEncoding == "UTF-8" &&
    props.getDeliveryMode == 2 // persistent
  }

  private def messageOfType[T : Manifest](validate: T => Boolean): Array[Byte] = argThat { body: Array[Byte] =>
    validate(Serialization.read[T](new String(body, StandardCharsets.UTF_8)))
  }
}
