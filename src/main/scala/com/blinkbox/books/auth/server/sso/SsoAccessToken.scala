package com.blinkbox.books.auth.server.sso

import java.io.IOException
import java.nio.file.{Files, Paths}
import java.security.spec.{InvalidKeySpecException, X509EncodedKeySpec}
import java.security.{InvalidKeyException, KeyFactory, NoSuchAlgorithmException, PublicKey}

import com.blinkbox.security.jwt.signatures.{ES256, SignatureAlgorithm}
import com.blinkbox.security.jwt.{InvalidTokenException, TokenDecoder, UnsupportedTokenException}

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag
import scala.util.Try

trait KeyStore {
  def verificationKey: PublicKey
}

class FileKeyStore(keyFile: String) extends KeyStore {
  private val DefaultKeyId = ""  // see SSO-369; i'm assuming we'll have more than one key in future
  private val verificationKeys = new TrieMap[String, PublicKey]()

  def verificationKey: PublicKey =
    // not using getOrElseUpdate as this is inherited from MapLike and isn't thread-safe
    // see this bug report for more info: https://issues.scala-lang.org/browse/SI-7943
    // also note that this method may read the file more than once, but that's alright
    verificationKeys.getOrElse(DefaultKeyId, try {
      val keyData = Files.readAllBytes(Paths.get(keyFile))
      val key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyData))
      verificationKeys.putIfAbsent(DefaultKeyId, key).getOrElse(key)
    } catch {
      case e: IOException => throw new InvalidTokenException(s"Failed to load key.")
      case e: NoSuchAlgorithmException => throw new UnsupportedTokenException("The ECDSA signature algorithm is not supported.", e)
      case e: InvalidKeySpecException => throw new InvalidKeyException("The public key is invalid.", e)
    })
}

class SsoAccessTokenDecoder(keyStore: KeyStore) extends TokenDecoder {
  override def getVerifier(header: java.util.Map[String, AnyRef]): SignatureAlgorithm =
    if (header.get("alg") == ES256.NAME) new ES256(keyStore.verificationKey)
    else super.getVerifier(header)
}

case class SsoAccessToken(token: String, claims: Map[String, AnyRef]) {
  val subject: String = claims.get("sub") match {
    case Some(s: String) => s
    case _ => throw new InvalidTokenException("The 'sub' claim is missing or invalid.")
  }
}

object SsoAccessToken {
  def decode(token: String, decoder: TokenDecoder): Try[SsoAccessToken] = Try {
    val claims = extractClaims[String, AnyRef](decoder.decode(token))
    SsoAccessToken(token, claims)
  }
  private def extractClaims[K: ClassTag, V: ClassTag](payload: Any): Map[K, V] = payload match {
    case claims: java.util.Map[K, V] => claims.asScala.toMap
    case _ => throw new InvalidTokenException("The token does not contain valid claims.")
  }
}
