package com.blinkbox.books.auth.server.events

import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data
import com.blinkbox.books.schemas.events.client.v2.{Client, ClientId}
import com.blinkbox.books.schemas.events.user.v2.{User, UserId}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.StoppedClock
import com.rabbitmq.client.{AMQP, Channel}
import com.blinkbox.books.json.DefaultFormats
import org.json4s.jackson.Serialization
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global

object RabbitMqPublisherTests {

  trait TestEnv extends MockitoSyrup {
    implicit val clock = StoppedClock()

    val userData = data.User(data.UserId(123), clock.now().minusDays(30), clock.now().minusDays(15), "john@example.org", "John", "Doe", "hash", allowMarketing = true)
    val clientData = data.Client(data.ClientId(456), clock.now().minusDays(29), clock.now().minusDays(14), userData.id, "Test Client", "Apple", "iPhone", "iOS", "secret", isDeregistered = false)

    val eventUser = User(UserId(userData.id.value), userData.username, userData.firstName, userData.lastName, userData.allowMarketing)
    val eventClient = Client(ClientId(clientData.id.value), clientData.name, clientData.brand, clientData.model, clientData.os)

    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
  }
}

class RabbitMqPublisherTests extends FlatSpec with BeforeAndAfter with ScalaFutures with MockitoSyrup {
  import RabbitMqPublisherTests._

  implicit override val patienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(20, Millis))
  implicit val formats = DefaultFormats

  "The publisher" should "declare an exchange with the correct parameters on instantiation" in new TestEnv {
    verify(channel).exchangeDeclare("Shop", "headers", true)
  }

  "The publisher" should "send a client deregistered message with the correct media type and payload" in new TestEnv {
    whenReady(publisher.publish(ClientDeregistered(clientData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.client.deregistered.v2+json"),
      messageOfType[Client.Registered] { msg => msg.timestamp == clientData.updatedAt && msg.client == eventClient })
    }
  }

  "The publisher" should "send a client registered message with the correct media type and payload" in new TestEnv {
    whenReady(publisher.publish(ClientRegistered(clientData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.client.registered.v2+json"),
      messageOfType[Client.Registered] { msg => msg.timestamp == clientData.createdAt && msg.client == eventClient })
    }
  }

  "The publisher" should "send a client updated message with the correct media type and payload" in new TestEnv {
    val newClientData = clientData.copy(updatedAt = clock.now().minusSeconds(1), name = "New Client")
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

  "The publisher" should "send a user authenticated message with the correct media type and payload, when there is no client" in new TestEnv {
    whenReady(publisher.publish(UserAuthenticated(userData, None))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.authenticated.v2+json"),
      messageOfType[User.Authenticated] { msg => msg.timestamp == clock.now() && msg.user == eventUser && msg.client == None })
    }
  }

  "The publisher" should "send a user authenticated message with the correct media type and payload, when there is a client" in new TestEnv {
    whenReady(publisher.publish(UserAuthenticated(userData, Some(clientData)))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.authenticated.v2+json"),
      messageOfType[User.Authenticated] { msg => msg.timestamp == clock.now() && msg.user == eventUser && msg.client == Some(eventClient) })
    }
  }

  "The publisher" should "send a user registered message with the correct media type and payload" in new TestEnv {
    whenReady(publisher.publish(UserRegistered(userData))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.registered.v2+json"),
      messageOfType[User.Registered] { msg => msg.timestamp == userData.createdAt && msg.user == eventUser })
    }
  }

  "The publisher" should "send a user updated message with the correct media type and payload" in new TestEnv {
    val newUserData = userData.copy(updatedAt = clock.now().minusSeconds(1), username = "fred@example.org", firstName = "Fred", lastName = "Bloggs")
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
