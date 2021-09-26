package me.james.chain.actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import me.james.chain.utils.{Loggable, PoWUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.chaining._

object Miner extends Loggable {
  sealed trait Command
  case class Mine(hash: String, proof: Long, replyTo: ActorRef[StatusReply[_]]) extends Command
  sealed trait MinerState
  case object Idle                                                              extends MinerState
  case object Mining                                                            extends MinerState

  private def ready: Behavior[Miner.Command] = Behaviors.receiveMessage {
    case Mine(hash, proof, replyTo) =>
      validate(hash, proof).pipe(beginMining).pipe(replyTo ! _)
      Behaviors.same
    case _                          => Behaviors.unhandled
  }

  def validate(hash: String, proof: Long): (Boolean, String) = {
    val isValid = PoWUtil.validProof(hash, proof)
    if (isValid) {
      logger.info("Proof is valid!")
    } else {
      logger.error("Proof is not valid!")
    }
    isValid -> hash
  }
  def beginMining(tupled: (Boolean, String)): StatusReply[_] = {
    val (isValid, hash) = tupled
    if (isValid) {
      logger.info("Begin mining")
      val eventualLong = Future {
        PoWUtil.pow(hash)
      }
      val calculated   = Await.result(eventualLong, Duration.Inf)
      StatusReply.success(calculated)
    } else {
      StatusReply.Error("Cancel Mining")
    }
  }

}
