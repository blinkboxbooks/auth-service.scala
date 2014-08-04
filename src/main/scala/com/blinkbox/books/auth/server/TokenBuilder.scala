package com.blinkbox.books.auth.server

import java.nio.file.{Paths, Files}
import java.security.KeyFactory
import java.security.spec.{X509EncodedKeySpec, PKCS8EncodedKeySpec}

import com.blinkbox.books.auth.server.data.{RefreshToken, Client, User}
import com.blinkbox.security.jwt.TokenEncoder
import com.blinkbox.security.jwt.encryption.{A128GCM, RSA_OAEP}
import com.blinkbox.security.jwt.signatures.ES256
import org.joda.time.{DateTimeZone, DateTime}

object TokenBuilder {
  // TODO: Make this configurable
  val PrivateKeyPath = "/opt/bbb/keys/blinkbox/zuul/sig/ec/1/private.key"

  def buildAccessToken(user: User, client: Option[Client], token: RefreshToken, expiresAt: DateTime) = {

    // TODO: Do this properly with configurable keys etc.

    val claims = new java.util.LinkedHashMap[String, AnyRef]
    claims.put("sub", user.id.external)
    claims.put("exp", Long.box(expiresAt.getMillis))
    client.foreach(c => claims.put("bb/cid", c.id.external))
    // TODO: Roles
    claims.put("zl/rti", Int.box(token.id.value))

    val signingKeyData = Files.readAllBytes(Paths.get(PrivateKeyPath))
    val signingKeySpec = new PKCS8EncodedKeySpec(signingKeyData)
    val signingKey = KeyFactory.getInstance("EC").generatePrivate(signingKeySpec)
    val signer = new ES256(signingKey)
    val signingHeaders = new java.util.LinkedHashMap[String, AnyRef]
    signingHeaders.put("kid", "/blinkbox/zuul/sig/ec/1")

    val encryptionKeyData = Files.readAllBytes(Paths.get("/opt/bbb/keys/blinkbox/plat/enc/rsa/1/public.key"))
    val encryptionKeySpec = new X509EncodedKeySpec(encryptionKeyData)
    val encryptionKey = KeyFactory.getInstance("RSA").generatePublic(encryptionKeySpec)
    val encryptionAlgorithm = new RSA_OAEP(encryptionKey)
    val encrypter = new A128GCM(encryptionAlgorithm)
    val encryptionHeaders = new java.util.LinkedHashMap[String, AnyRef]
    encryptionHeaders.put("kid", "/blinkbox/plat/enc/rsa/1")
    encryptionHeaders.put("cty", "JWT")

    val encoder = new TokenEncoder()
    val signed = encoder.encode(claims, signer, signingHeaders)
    val encrypted = encoder.encode(signed, encrypter, encryptionHeaders)

    encrypted
  }

  def issueAccessToken(user: User, client: Option[Client], token: RefreshToken, includeRefreshToken: Boolean = false, includeClientSecret: Boolean = false): TokenInfo = {
    val expiresAt = DateTime.now(DateTimeZone.UTC).plusSeconds(1800)
    TokenInfo(
      access_token = buildAccessToken(user, client, token, expiresAt),
      token_type = "bearer",
      expires_in = 1800,
      refresh_token = if (includeRefreshToken) Some(token.token) else None,
      user_id = user.id.external,
      user_uri = s"/users/${user.id.value}",
      user_username = user.username,
      user_first_name = user.firstName,
      user_last_name = user.lastName,
      client_id = client.map(row => row.id.external),
      client_uri = client.map(row => s"/clients/${row.id.value}"),
      client_name = client.map(_.name),
      client_brand = client.map(_.brand),
      client_model = client.map(_.model),
      client_os = client.map(_.os),
      client_secret = if (includeClientSecret) client.map(_.secret) else None,
      last_used_date = client.map(_.updatedAt))
  }
}
