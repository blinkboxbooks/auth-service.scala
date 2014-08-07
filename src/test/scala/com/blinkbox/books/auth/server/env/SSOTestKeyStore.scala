package com.blinkbox.books.auth.server.env

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}

import com.blinkbox.books.auth.server.sso.KeyStore

object SSOTestKeyStore extends KeyStore {
  def verificationKey: PublicKey = {
    val keyStream = getClass.getClassLoader.getResourceAsStream("public.key")
    val keyData = Stream.continually(keyStream.read).takeWhile(_ != -1).map(_.toByte).toArray
    KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyData))
  }
}
