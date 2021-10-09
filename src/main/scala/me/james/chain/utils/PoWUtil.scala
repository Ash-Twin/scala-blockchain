package me.james.chain.utils

import io.circe.syntax.EncoderOps

import scala.annotation.tailrec

object PoWUtil {

  def pow(lastHash: String): Long = {
    @tailrec
    def powHelper(lastHash: String, proof: Long): Long =
      if (validProof(lastHash, proof)) {
        proof
      } else {
        powHelper(lastHash, proof + 1)
      }
    powHelper(lastHash, 0)
  }

  def validProof(lastHash: String, proof: Long): Boolean = {
    val guess     = (lastHash ++ proof.toString).asJson.noSpaces
    val guessHash = CryptoUtil.sha256Hash(guess)
    (guessHash take 4) == "0000"
  }

}
