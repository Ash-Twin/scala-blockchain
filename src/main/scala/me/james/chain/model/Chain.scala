package me.james.chain.model

import akka.protobufv3.internal.EmptyOrBuilder
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo, JsonTypeName}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import me.james.chain.utils.CryptoUtil

import java.security.InvalidParameterException
import java.time.LocalDateTime
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[EmptyChain], name = "empty_chain"),
    new JsonSubTypes.Type(value = classOf[ChainLink], name = "chain_link")))
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

object Chain {
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
    tail: Chain = EmptyChain,
    timestamp: LocalDateTime = LocalDateTime.now()
) extends Chain {
  override val hash: String = CryptoUtil.sha256Hash(this.asJson.noSpaces)
}

/**
 * ADT with trait and case object
 *
 * It’s common in Scala to use a sealed trait and case objects to represent enums. If the values are case classes the @JsonSubTypes annotation as described above works, but if the values are case objects it will not. The annotation requires a Class and there is no way to define that in an annotation for a case object.
 * The easiest workaround is to define the case objects as case class without any field.
 * Alternatively, you can define an intermediate trait for the case object and a custom deserializer for it. The example below builds on the previous Animal sample by adding a fictitious, single instance, new animal, an Unicorn.
 */
@JsonDeserialize(using = classOf[EmptyChainDeserializer])
sealed trait EmptyChain extends Chain
@JsonTypeName("empty_chain")
case object EmptyChain extends EmptyChain {
  override val index: Int                = 0
  override val hash: String              = "1"
  override val values: List[Transaction] = Nil
  override val proof: Long               = 100L
  override val timestamp: LocalDateTime  = LocalDateTime.now()
}

class EmptyChainDeserializer extends StdDeserializer[EmptyChain](EmptyChain.getClass){
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): EmptyChain = EmptyChain
}