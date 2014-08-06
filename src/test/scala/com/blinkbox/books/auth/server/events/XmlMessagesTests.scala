package com.blinkbox.books.auth.server.events

import com.blinkbox.books.auth.server.data.{Client, ClientId, User, UserId}
import com.blinkbox.books.time.StoppedClock
import org.custommonkey.xmlunit.{Diff, XMLUnit}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FunSuite

class XmlMessagesTests extends FunSuite {
  implicit val clock = StoppedClock(new DateTime(2014, 7, 14, 11, 58, 32, 837, DateTimeZone.UTC))

  XMLUnit.setIgnoreWhitespace(true)
  XMLUnit.setIgnoreComments(true)

  test("The user registered XML message is correct") {
    val user = User(UserId(123), clock.now(), clock.now(), "newuser@example.org", "John", "Doe", "hash", allowMarketing = true)
    val message = XmlMessages.userRegistered(user)
    val expected =
      <registered
        xmlns="http://schemas.blinkboxbooks.com/events/users/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <user>
          <id>123</id>
          <username>newuser@example.org</username>
          <firstName>John</firstName>
          <lastName>Doe</lastName>
          <allowMarketingCommunications>true</allowMarketingCommunications>
        </user>
      </registered>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The user updated XML message is correct") {
    val oldUser = User(UserId(123), clock.now(), clock.now(), "olduser@example.org", "John1", "Doe1", "hash", allowMarketing = true)
    val newUser = User(UserId(123), clock.now(), clock.now(), "newuser@example.org", "John2", "Doe2", "hash", allowMarketing = false)
    val message = XmlMessages.userUpdated(oldUser, newUser)
    val expected =
      <updated
        xmlns="http://schemas.blinkboxbooks.com/events/users/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <userId>123</userId>
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <old>
          <username>olduser@example.org</username>
          <firstName>John1</firstName>
          <lastName>Doe1</lastName>
          <allowMarketingCommunications>true</allowMarketingCommunications>
        </old>
        <new>
          <username>newuser@example.org</username>
          <firstName>John2</firstName>
          <lastName>Doe2</lastName>
          <allowMarketingCommunications>false</allowMarketingCommunications>
        </new>
      </updated>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The user authenticated XML message without a client is correct") {
    val user = User(UserId(123), clock.now(), clock.now(), "newuser@example.org", "John", "Doe", "hash", allowMarketing = true)
    val message = XmlMessages.userAuthenticated(user, None)
    val expected =
      <authenticated
        xmlns="http://schemas.blinkboxbooks.com/events/users/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <user>
          <id>123</id>
          <username>newuser@example.org</username>
          <firstName>John</firstName>
          <lastName>Doe</lastName>
          <allowMarketingCommunications>true</allowMarketingCommunications>
        </user>
      </authenticated>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The user authenticated XML message with a client is correct") {
    val user = User(UserId(123), clock.now(), clock.now(), "newuser@example.org", "John", "Doe", "hash", allowMarketing = true)
    val client = Client(ClientId(456), clock.now(), clock.now(), UserId(123), "test client", "Apple", "iPhone 5S", "iOS 7.1.2", "very secret", isDeregistered = false)
    val message = XmlMessages.userAuthenticated(user, Some(client))
    val expected =
      <authenticated
        xmlns="http://schemas.blinkboxbooks.com/events/users/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <user>
          <id>123</id>
          <username>newuser@example.org</username>
          <firstName>John</firstName>
          <lastName>Doe</lastName>
          <allowMarketingCommunications>true</allowMarketingCommunications>
        </user>
        <client>
          <id>456</id>
          <name>test client</name>
          <brand>Apple</brand>
          <model>iPhone 5S</model>
          <os>iOS 7.1.2</os>
        </client>
      </authenticated>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The client registered XML message is correct") {
    val client = Client(ClientId(456), clock.now(), clock.now(), UserId(123), "test client", "Apple", "iPhone 5S", "iOS 7.1.2", "very secret", isDeregistered = false)
    val message = XmlMessages.clientRegistered(client)
    val expected =
      <registered
        xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <userId>123</userId>
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <client>
          <id>456</id>
          <name>test client</name>
          <brand>Apple</brand>
          <model>iPhone 5S</model>
          <os>iOS 7.1.2</os>
        </client>
      </registered>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The client updated XML message is correct") {
    val oldClient = Client(ClientId(456), clock.now(), clock.now(), UserId(123), "test client", "Apple", "iPhone 5S", "iOS 7.1.2", "very secret", isDeregistered = false)
    val newClient = Client(ClientId(456), clock.now(), clock.now(), UserId(123), "My Client", "Samsung", "Galaxy S4", "Android 4.2", "still secret", isDeregistered = false)
    val message = XmlMessages.clientUpdated(oldClient, newClient)
    val expected =
      <updated
        xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <userId>123</userId>
        <clientId>456</clientId>
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <old>
          <name>test client</name>
          <brand>Apple</brand>
          <model>iPhone 5S</model>
          <os>iOS 7.1.2</os>
        </old>
        <new>
          <name>My Client</name>
          <brand>Samsung</brand>
          <model>Galaxy S4</model>
          <os>Android 4.2</os>
        </new>
      </updated>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

  test("The client deregistered XML message is correct") {
    val client = Client(ClientId(456), clock.now(), clock.now(), UserId(123), "test client", "Apple", "iPhone 5S", "iOS 7.1.2", "very secret", isDeregistered = true)
    val message = XmlMessages.clientDeregistered(client)
    val expected =
      <deregistered
        xmlns="http://schemas.blinkboxbooks.com/events/clients/v1"
        xmlns:r="http://schemas.blinkboxbooks.com/messaging/routing/v1"
        xmlns:v="http://schemas.blinkboxbooks.com/messaging/versioning"
        r:originator="zuul" v:version="1.0">
        <userId>123</userId>
        <timestamp>2014-07-14T11:58:32.837Z</timestamp>
        <client>
          <id>456</id>
          <name>test client</name>
          <brand>Apple</brand>
          <model>iPhone 5S</model>
          <os>iOS 7.1.2</os>
        </client>
      </deregistered>
    val diff = new Diff(expected.toString(), message.toString())
    assert(diff.similar(), diff.toString)
  }

}
