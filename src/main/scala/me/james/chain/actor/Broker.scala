package me.james.chain.actor

import akka.Done
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import me.james.chain.model.Transaction

import scala.collection.mutable

object Broker {
  sealed trait Command
  case class AddTransaction(transaction: Transaction, replyTo: ActorRef[StatusReply[Done]]) extends Command
  case class GetTransactions(replyTo: ActorRef[StatusReply[List[Transaction]]])             extends Command
  case class Clear(replyTo: ActorRef[StatusReply[Int]])                                     extends Command
  def apply(): Behavior[Broker.Command] = Behaviors
    .supervise(Behaviors.setup[Command] { ctx =>
      new Broker(ctx)
    })
    .onFailure(SupervisorStrategy.restart)
}
class Broker(context: ActorContext[Broker.Command]) extends AbstractBehavior[Broker.Command](context) {

  private val pending: mutable.ListBuffer[Transaction] = mutable.ListBuffer.empty

  override def onMessage(msg: Broker.Command): Behavior[Broker.Command] = msg match {
    case Broker.AddTransaction(transaction, replyTo) =>
      pending += transaction
      replyTo ! StatusReply.Ack
      Behaviors.same
    case Broker.GetTransactions(replyTo)             =>
      replyTo ! StatusReply.success(pending.toList)
      Behaviors.same
    case Broker.Clear(replyTo)                       =>
      val size = pending.size
      pending.clear()
      replyTo ! StatusReply.success(size)
      Behaviors.same
    case _                                           =>
      Behaviors.unhandled
  }

}
