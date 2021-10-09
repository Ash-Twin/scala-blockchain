package me.james.chain.utils

import java.math.BigInteger
import java.security.MessageDigest

object CryptoUtil {
  def sha256Hash(str: String): String =
    String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8"))))
}
