package me.james.chain.actor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import me.james.chain.actor.Miner.beginMining
import me.james.chain.utils.{Loggable, PoWUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.chaining._

object Miner extends Loggable {

  def beginMining(hash: String): StatusReply[Long] = {
    logger.info("Begin mining")
    val eventualLong = Future {
      PoWUtil.pow(hash)
    }
    StatusReply.success(Await.result(eventualLong, Duration.Inf))
  }

  def apply(): Behavior[Miner.Command] = Behaviors
    .supervise(
      Behaviors.withMdc(
        staticMdc = Map.empty,
        (msg: Command) => Map(this.getClass.getSimpleName -> msg.getClass.getSimpleName)
      ) {
        Behaviors.setup[Command] { ctx =>
          new Miner(ctx)
        }
      }
    )
    .onFailure(SupervisorStrategy.restart)

  sealed trait Command

  case class Mine(hash: String, replyTo: ActorRef[StatusReply[Long]]) extends Command

}
class Miner(context: ActorContext[Miner.Command]) extends AbstractBehavior[Miner.Command](context) {
  override def onMessage(msg: Miner.Command): Behavior[Miner.Command] = msg match {
    case Miner.Mine(hash, replyTo) =>
      beginMining(hash).pipe(replyTo ! _)

      Behaviors.same
    case _                         =>
      Behaviors.unhandled
  }

}
