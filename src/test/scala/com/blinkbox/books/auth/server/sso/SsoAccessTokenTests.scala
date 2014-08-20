package com.blinkbox.books.auth.server.sso

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Date

import com.blinkbox.security.jwt.InvalidTokenException
import org.scalatest.FunSuite

import scala.util.{Failure, Success}

private object TestKeyStore extends KeyStore {
  def verificationKey: PublicKey = {
    val keyStream = getClass.getClassLoader.getResourceAsStream("sso_public.key")
    val keyData = Stream.continually(keyStream.read).takeWhile(_ != -1).map(_.toByte).toArray
    KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyData))
  }
}

class SsoAccessTokenTests extends FunSuite {

  test("Can decode a token with a valid signature") {
    val token = "eyJhbGciOiJFUzI1NiJ9.eyJzY3AiOlsic3NvOmJvb2tzIl0sImV4cCI6MTQwNjU1NjU5OSwic3ViIjoiQjBFODQyOEUtN0RFQi00MEJGLUJGQkUtNUQwOTI3QTU0RjY1IiwicmlkIjoiNEY3N0M1RkEtNTJCQy00RDY0LUI0OUItOTMyNUY3ODE1NEYwIiwibG5rIjpbXSwic3J2IjoiYm9va3MiLCJyb2wiOltdLCJ0a3QiOiJhY2Nlc3MiLCJpYXQiOjE0MDY1NTQ3OTl9.lTtM96tL9ALtZPd8Ct28dt4BinWuru6L-nXqMANro14N0SKcOJhJppfEOC2y8CUEQ_XN55WA2IdTm1ebIUV9gQ"
    val decoder: SsoAccessTokenDecoder = new SsoAccessTokenDecoder(TestKeyStore) {
      override def validateExpirationTime(expirationTime: Date) = {} // allow expired token for test purposes
    }
    SsoDecodedAccessToken.decode(token, decoder) match {
      case Failure(e) => fail(e)
      case Success(t) =>
        assert(t.token == token)
        assert(t.claims.size == 9) // could go into more detail if we wanted
    }
  }

  test("Fails to decode a token with an invalid signature") {
    // note: just the last letter of the token has been changed
    val token = "eyJhbGciOiJFUzI1NiJ9.eyJzY3AiOlsic3NvOmJvb2tzIl0sImV4cCI6MTQwNjU1NjU5OSwic3ViIjoiQjBFODQyOEUtN0RFQi00MEJGLUJGQkUtNUQwOTI3QTU0RjY1IiwicmlkIjoiNEY3N0M1RkEtNTJCQy00RDY0LUI0OUItOTMyNUY3ODE1NEYwIiwibG5rIjpbXSwic3J2IjoiYm9va3MiLCJyb2wiOltdLCJ0a3QiOiJhY2Nlc3MiLCJpYXQiOjE0MDY1NTQ3OTl9.lTtM96tL9ALtZPd8Ct28dt4BinWuru6L-nXqMANro14N0SKcOJhJppfEOC2y8CUEQ_XN55WA2IdTm1ebIUV9gD"
    val result = SsoDecodedAccessToken.decode(token, new SsoAccessTokenDecoder(TestKeyStore))
    result match {
      case Success(_) => fail("Should not decode a token with an invalid signature")
      case Failure(e) =>
        assert(e.isInstanceOf[InvalidTokenException])
        assert(e.getMessage == "The signature is invalid.")
    }
  }

}
