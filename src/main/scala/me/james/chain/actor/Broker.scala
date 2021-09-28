package me.james.chain.actor

import akka.Done
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import me.james.chain.model.Transaction
import me.james.chain.utils.Loggable

import scala.collection.mutable

object Broker {
  def apply(): Behavior[Broker.Command] = Behaviors
    .supervise(
      Behaviors.withMdc(
        staticMdc = Map.empty,
        (msg: Broker.Command) => Map(" <" -> s" ${msg.getClass.getSimpleName}")
      ) {
        Behaviors.setup[Broker.Command] { ctx =>
          new Broker(ctx)
        }
      }
    )
    .onFailure(SupervisorStrategy.restart)

  sealed trait Command

  case class AddTransaction(transaction: Transaction, replyTo: ActorRef[StatusReply[Done]]) extends Command

  case class GetTransactions(replyTo: ActorRef[StatusReply[List[Transaction]]]) extends Command

  case class Clear(replyTo: ActorRef[StatusReply[Int]]) extends Command
}
class Broker(context: ActorContext[Broker.Command]) extends AbstractBehavior[Broker.Command](context) with Loggable {

  private val pending: mutable.ListBuffer[Transaction] = mutable.ListBuffer.empty

  override def onMessage(msg: Broker.Command): Behavior[Broker.Command] =
    msg match {
      case Broker.AddTransaction(transaction, replyTo) =>
        logger.info(transaction.toString)
        pending += transaction
        replyTo ! StatusReply.Ack
        Behaviors.same
      case Broker.GetTransactions(replyTo)             =>
        logger.info(s"Pending: ${pending.size} "+pending.mkString(" | "))
        replyTo ! StatusReply.success(pending.toList)
        Behaviors.same
      case Broker.Clear(replyTo)                       =>
        val size = pending.size
        pending.clear()
        logger.info(s"Clear $size pending transactions")
        replyTo ! StatusReply.success(size)
        Behaviors.same
      case _                                           =>
        Behaviors.unhandled
    }

}
