package me.james.chain.actor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import me.james.chain.actor.Miner.{beginMining, validate}
import me.james.chain.utils.{Loggable, PoWUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.chaining._

object Miner extends Loggable {
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
      StatusReply.success(Await.result(eventualLong, Duration.Inf))
    } else {
      StatusReply.Error("Cancel Mining")
    }
  }

  def apply(): Behavior[Miner.Command] = Behaviors
    .supervise(Behaviors.setup[Command] { ctx =>
      new Miner(ctx)
    })
    .onFailure(SupervisorStrategy.restart)

  sealed trait Command

  case class Mine(hash: String, proof: Long, replyTo: ActorRef[StatusReply[_]]) extends Command

}
class Miner(context: ActorContext[Miner.Command]) extends AbstractBehavior[Miner.Command](context) {
  override def onMessage(msg: Miner.Command): Behavior[Miner.Command] = msg match {
    case Miner.Mine(hash, proof, replyTo) =>
      validate(hash, proof).pipe(beginMining).pipe(replyTo ! _)

      Behaviors.same
    case _                                =>
      Behaviors.unhandled
  }

}
