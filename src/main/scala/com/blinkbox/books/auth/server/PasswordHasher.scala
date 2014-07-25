package com.blinkbox.books.auth.server

import com.lambdaworks.crypto.SCryptUtil

trait PasswordHasher {
  def check(password: String, hash: String): Boolean

  def hash(password: String): String
}

object PasswordHasher {
  lazy val default = new PasswordHasher {
    val n = 16384
    val r = 8
    val p = 1

    override def check(password: String, hash: String) = SCryptUtil.check(password, hash)
    override def hash(password: String) = SCryptUtil.scrypt(password, n, r, p)
  }

  def apply(hashFn: String => String, checkFn: (String, String) => Boolean) = new PasswordHasher {
    override def check(password: String, hash: String) = checkFn(password, hash)
    override def hash(password: String): String = hashFn(password)
  }
}
