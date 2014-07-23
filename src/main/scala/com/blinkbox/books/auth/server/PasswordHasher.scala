package com.blinkbox.books.auth.server

import com.lambdaworks.crypto.SCryptUtil

trait PasswordHasher {
  def hash(password: String): String
}

object PasswordHasher {
  lazy val default = new PasswordHasher {
    val n = 16384
    val r = 8
    val p = 1

    override def hash(password: String) = SCryptUtil.scrypt(password, n, r, p)
  }

  def apply(hashFn: String => String) = new PasswordHasher {
    override def hash(password: String): String = hashFn(password)
  }
}
