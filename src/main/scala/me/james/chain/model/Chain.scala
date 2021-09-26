package me.james.chain.model

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import me.james.chain.utils.CryptoUtil

import java.security.InvalidParameterException
import java.time.LocalDateTime
sealed trait Chain {
  val index: Int
  val hash: String
  val values: List[Transaction]
  val proof: Long
  val timestamp: LocalDateTime

  def ::(link: Chain): Chain = link match {
    case ChainLink(index, proof, values, _, _, _) => ChainLink(index, proof, values, hash, this)
    case _                                        => throw new InvalidParameterException("Cannot add to an invalid link")
  }
}
object Chain       {
  def apply[T](b: Chain*): Chain =
    if (b.isEmpty) EmptyChain
    else {
      val link = b.head.asInstanceOf[ChainLink]
      ChainLink(link.index, link.proof, link.values, link.previousHash, apply(b.tail: _*))
    }
}

case class ChainLink(
    index: Int,
    proof: Long,
    values: List[Transaction],
    previousHash: String = "",
    tail: Chain,
    timestamp: LocalDateTime = LocalDateTime.now()
) extends Chain {
  override val hash: String = CryptoUtil.sha256Hash(this.asJson.noSpaces)
}

case object EmptyChain extends Chain {
  override val index: Int                = 0
  override val hash: String              = "1"
  override val values: List[Transaction] = Nil
  override val proof: Long               = 100L
  override val timestamp: LocalDateTime  = LocalDateTime.now()
}
