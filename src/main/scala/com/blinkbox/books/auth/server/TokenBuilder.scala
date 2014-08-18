package com.blinkbox.books.auth.server

import java.nio.file.{Files, Paths}
import java.security.KeyFactory
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import com.blinkbox.books.auth.server.data.{Client, RefreshToken, User}
import com.blinkbox.books.auth.server.sso.SSOCredentials
import com.blinkbox.security.jwt.TokenEncoder
import com.blinkbox.security.jwt.encryption.{A128GCM, RSA_OAEP}
import com.blinkbox.security.jwt.signatures.ES256
import org.joda.time.{DateTimeZone, DateTime}

object TokenBuilder {
  // TODO: Make this configurable
  val SigningKeyPath = "/opt/bbb/keys/blinkbox/zuul/sig/ec/1/private.key"
  val EncryptionKeyPath = "/opt/bbb/keys/blinkbox/plat/enc/rsa/1/public.key"

  def buildAccessToken(user: User, client: Option[Client], token: RefreshToken, ssoCredentials: Option[SSOCredentials]): (Long, String) = {

    // TODO: Do this properly with configurable keys etc.

    // Expires our token 1 minute before the SSO ones, or in 30 minutes if no SSO credentials are provided
    val expiresIn = ssoCredentials.map(_.expiresIn - 60).getOrElse(1800)

    val claims = new java.util.LinkedHashMap[String, AnyRef]
    claims.put("sub", user.id.external)
    claims.put("exp", DateTime.now(DateTimeZone.UTC).plusSeconds(expiresIn))

    // Stores the SSO access token within the Zuul one if provided
    ssoCredentials.foreach(c => claims.put("sso/at", c.accessToken))

    client.foreach(c => claims.put("bb/cid", c.id.external))
    // TODO: Roles
    claims.put("zl/rti", Int.box(token.id.value))

    val signingKeyData = Files.readAllBytes(Paths.get(SigningKeyPath))
    val signingKeySpec = new PKCS8EncodedKeySpec(signingKeyData)
    val signingKey = KeyFactory.getInstance("EC").generatePrivate(signingKeySpec)
    val signer = new ES256(signingKey)
    val signingHeaders = new java.util.LinkedHashMap[String, AnyRef]
    signingHeaders.put("kid", "/blinkbox/zuul/sig/ec/1")

    val encryptionKeyData = Files.readAllBytes(Paths.get(EncryptionKeyPath))
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

    (expiresIn, encrypted)
  }

  def issueAccessToken(
      user: User,
      client: Option[Client],
      token: RefreshToken,
      ssoCredentials: Option[SSOCredentials],
      includeRefreshToken: Boolean = false,
      includeClientSecret: Boolean = false): TokenInfo = {

    val (expiresIn, accessToken) = buildAccessToken(user, client, token, ssoCredentials)

    TokenInfo(
      access_token = accessToken,
      token_type = "bearer",
      expires_in = expiresIn,
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
