package com.blinkbox.books.auth.server.events

import java.net.InetAddress
import java.util.UUID

import com.blinkbox.books.auth.server.data.{Client, User}
import com.blinkbox.books.time.Clock

private[events] object XmlMessages {
  private val UsersNamespace = "http://schemas.blinkboxbooks.com/events/users/v1"
  private val ClientsNamespace = "http://schemas.blinkboxbooks.com/events/clients/v1"
  private val EmailNamespace = "http://schemas.blinkbox.com/books/emails/sending/v1"
  private val RoutingNamespace = "http://schemas.blinkboxbooks.com/messaging/routing/v1"
  private val VersionNamespace = "http://schemas.blinkboxbooks.com/messaging/versioning"

  def userRegistered(user: User)(implicit clock: Clock) =
    <registered xmlns={UsersNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      {timestampXml}
      {userXml(user, includeId = true)}
    </registered>

  def userUpdated(oldUser: User, newUser: User)(implicit clock: Clock) =
    <updated xmlns={UsersNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      <userId>{oldUser.id.value}</userId>
      {timestampXml}
      {userXml(oldUser, elementName = "old")}
      {userXml(newUser, elementName = "new")}
    </updated>

  def userAuthenticated(user: User, client: Option[Client])(implicit clock: Clock) =
    <authenticated xmlns={UsersNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      {timestampXml}
      {userXml(user, includeId = true)}
      {if (client.isDefined) clientXml(client.get, includeId = true)}
    </authenticated>

  def clientRegistered(client: Client)(implicit clock: Clock) =
    <registered xmlns={ClientsNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      <userId>{client.userId.value}</userId>
      {timestampXml}
      {clientXml(client, includeId = true)}
    </registered>

  def clientUpdated(oldClient: Client, newClient: Client)(implicit clock: Clock) =
    <updated xmlns={ClientsNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      <userId>{oldClient.userId.value}</userId>
      <clientId>{oldClient.id.value}</clientId>
      {timestampXml}
      {clientXml(oldClient, elementName = "old")}
      {clientXml(newClient, elementName = "new")}
    </updated>

  def clientDeregistered(client: Client)(implicit clock: Clock) =
    <deregistered xmlns={ClientsNamespace} xmlns:r={RoutingNamespace} xmlns:v={VersionNamespace} r:originator="zuul" v:version="1.0">
      <userId>{client.userId.value}</userId>
      {timestampXml}
      {clientXml(client, includeId = true)}
    </deregistered>

  def sendEmail(user: User, template: String, variables: Map[String, AnyRef], messageId: String = UUID.randomUUID.toString) =
    <sendEmail xmlns={EmailNamespace} xmlns:r="http://schemas.blinkbox.com/books/routing/v1" r:originator="zuul" r:instance={InetAddress.getLocalHost.getHostName} r:messageId={messageId}>
      <template>{template}</template>
      <to>
        <recipient>
          <name>{s"${user.firstName} ${user.lastName}"}</name>
          <email>{user.username}</email>
        </recipient>
      </to>
      <templateVariables>
        {for ((k, v) <- variables) yield
        <templateVariable>
          <key>{k}</key>
          <value>{v}</value>
        </templateVariable>}
      </templateVariables>
    </sendEmail>

  private def timestampXml(implicit clock: Clock) = <timestamp>{clock.now()}</timestamp>

  private def userXml(user: User, elementName: String = "user", includeId: Boolean = false) =
    <user>
      {if (includeId) <id>{user.id.value}</id>}
      <username>{user.username}</username>
      <firstName>{user.firstName}</firstName>
      <lastName>{user.lastName}</lastName>
      <allowMarketingCommunications>{user.allowMarketing}</allowMarketingCommunications>
    </user>.copy(label = elementName)

  private def clientXml(client: Client, elementName: String = "client", includeId: Boolean = false) =
    <client>
      {if (includeId) <id>{client.id.value}</id>}
      <name>{client.name}</name>
      <brand>{client.brand}</brand>
      <model>{client.model}</model>
      <os>{client.os}</os>
    </client>.copy(label = elementName)
}
