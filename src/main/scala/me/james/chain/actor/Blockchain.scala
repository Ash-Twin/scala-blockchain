package me.james.chain.actor

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import me.james.chain.config.AppConfig
import me.james.chain.model.{Chain, ChainLink, EmptyChain, Transaction}

import java.util.UUID

object Blockchain {
  def apply(config: AppConfig, actorSys: ActorSystem[_]): Behavior[Blockchain.Command[_]] =
    Behaviors
      .supervise(
        Behaviors.withMdc[Blockchain.Command[_]](
          Map.empty[String,String],
          (msg: Blockchain.Command[_]) => Map(" <" -> s" ${msg.getClass.getSimpleName}")
        ) {
          Blockchain.counter(PersistenceId.ofUniqueId(config.persistenceId))
        }
      )
      .onFailure(SupervisorStrategy.restart)

  def counter(persistenceId: PersistenceId): EventSourcedBehavior[Blockchain.Command[_],Blockchain.Event, State] =
    EventSourcedBehavior.withEnforcedReplies[Blockchain.Command[_], Blockchain.Event,State](
      persistenceId,
      emptyState = State(EmptyChain),
      commandHandler = (state, command) => {
        command match {
          case AddBlock(transactions, proof, replyTo) =>
            Effect
              .persist(BlockAdded(ChainLink(state.chain.index + 1, proof, transactions) :: state.chain))
              .thenReply(replyTo)(_ => StatusReply.Ack)
          case GetChain(replyTo)                      =>
            Effect.reply(replyTo)(StatusReply.success(state.chain))
          case GetLastHash(replyTo)                   =>
            Effect.reply(replyTo)(StatusReply.success(state.chain.hash))
          case GetLastIndex(replyTo)                  =>
            Effect.reply(replyTo)(StatusReply.success(state.chain.index))
        }
      },
      eventHandler = (state, event) => state.applyEvent(event)
    )

  sealed trait Command[ReplyMessage] extends CborSerializable

  case class AddBlock(transactions: List[Transaction], proof: Long, replyTo: ActorRef[StatusReply[Done]])
      extends Command[Done]

  case class GetChain(replyTo: ActorRef[StatusReply[Chain]]) extends Command[Chain]

  case class GetLastHash(replyTo: ActorRef[StatusReply[String]]) extends Command[String]

  case class GetLastIndex(replyTo: ActorRef[StatusReply[Int]]) extends Command[Int]

  sealed trait Event extends CborSerializable

  case class BlockAdded(chain:Chain) extends Event

  case class State(chain: Chain) extends CborSerializable {
    def applyEvent(event: Event): State = event match {
      case BlockAdded(chain) => State(chain)
    }
  }
}
