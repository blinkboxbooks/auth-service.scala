package com.blinkbox.books.auth.server.events

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.blinkbox.books.auth.server.data
import com.blinkbox.books.schemas.events.user.v2.{UserId, User}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.{StoppedClock, SystemTimeSupport}
import com.rabbitmq.client.{AMQP, Channel}
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, DefaultFormats}
import org.json4s.jackson.{Serialization, JsonMethods}
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
  
  val dataUser = data.User(data.UserId(123), clock.now(), clock.now(), "john@example.org", "John", "Doe", "hash", allowMarketing = true)
  val eventUser =  User(UserId(123), "john@example.org", "John", "Doe", allowMarketingCommunications = true)
  
  test("Declares an exchange with the correct parameters on instantiation") {
    val channel = mock[Channel]
    new RabbitMqPublisher(channel)
    verify(channel).exchangeDeclare("Shop", "headers", true)
  }

  test("Sends user registered messages with the correct media type and payload") {
    val channel = mock[Channel]
    val publisher = new RabbitMqPublisher(channel)
    whenReady(publisher.publish(UserRegistered(dataUser))) { _ => verify(channel).basicPublish(
      shopExchange,
      noRoutingKey,
      propertiesFor("application/vnd.blinkbox.books.events.user.registered.v2+json"),
      messageOfType[User.Registered] { msg => msg.timestamp == clock.now() && msg.user == eventUser })
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
