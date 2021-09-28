package me.james.chain.actor

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import me.james.chain.config.AppConfig
import me.james.chain.model.{Chain, ChainLink, EmptyChain, Transaction}

object Blockchain {
  def apply(config: AppConfig, actorSys: ActorSystem[_]): Behavior[Blockchain.Command[_]] =
    Behaviors
      .supervise(
        Blockchain.counter(PersistenceId.ofUniqueId(config.persistenceId))
      )
      .onFailure(SupervisorStrategy.restart)

  def counter(persistenceId: PersistenceId): DurableStateBehavior[Blockchain.Command[_], State] =
    DurableStateBehavior.withEnforcedReplies[Blockchain.Command[_], State](
      persistenceId,
      emptyState = State(EmptyChain),
      commandHandler = (state, command) => {
        command match {
          case AddBlock(transactions, proof, replyTo) =>
            Effect
              .persist(State(ChainLink(state.chain.index + 1, proof, transactions) :: state.chain))
              .thenReply(replyTo)(_ => StatusReply.Ack)
          case GetChain(replyTo)                      =>
            Effect.reply(replyTo)(state.chain)
          case GetLastHash(replyTo)                   =>
            Effect.reply(replyTo)(StatusReply.success(state.chain.hash))
          case GetLastIndex(replyTo)                  =>
            Effect.reply(replyTo)(StatusReply.success(state.chain.index))
        }
      }
    )

  sealed trait Command[ReplyMessage]

  case class AddBlock(transactions: List[Transaction], proof: Long, replyTo: ActorRef[StatusReply[Done]])
      extends Command[Done]

  case class GetChain(replyTo: ActorRef[Chain])                  extends Command[Chain]

  case class GetLastHash(replyTo: ActorRef[StatusReply[String]]) extends Command[String]

  case class GetLastIndex(replyTo: ActorRef[StatusReply[Int]])   extends Command[Int]

  case class State(chain: Chain)
}
