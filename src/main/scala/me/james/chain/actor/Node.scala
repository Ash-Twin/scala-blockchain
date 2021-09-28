package me.james.chain.actor

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.util.Timeout
import me.james.chain.model.Transaction
import me.james.chain.utils.{Loggable, PoWUtil}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Node       {
  def apply(
      blockchain: ActorRef[Blockchain.Command[_]],
      miner: ActorRef[Miner.Command],
      broker: ActorRef[Broker.Command]
  ): Behavior[Node.Command] = Behaviors
    .supervise(
      Behaviors
        .withMdc[Node.Command](Map.empty[String,String], (msg: Node.Command) => Map(" <" -> s" ${msg.getClass.getSimpleName}")) {
          Behaviors.setup[Node.Command] { ctx =>
            new Node(ctx, blockchain, miner, broker)
          }
        }
    )
    .onFailure(SupervisorStrategy.restart)

  sealed trait Command

  case class AddTransaction(transaction: Transaction, replyTo: ActorRef[StatusReply[Done]]) extends Command

  case class CheckPowSolution(solution: Long, replyTo: ActorRef[StatusReply[_]]) extends Command

  case class AddBlock(proof: Long, replyTo: ActorRef[StatusReply[_]]) extends Command

  case class GetTransactions(replyTo: ActorRef[StatusReply[List[Transaction]]]) extends Command

  case object StartMining extends Command

  case class StopMining(replyTo: ActorRef[StatusReply[_]]) extends Command

  case class GetStatus(replyTo: ActorRef[StatusReply[_]]) extends Command

  case class GetLastBlockIndex(replyTo: ActorRef[StatusReply[_]]) extends Command

  case class GetLastBlockHash(replyTo: ActorRef[StatusReply[_]]) extends Command
}
class Node(
    context: ActorContext[Node.Command],
    blockchain: ActorRef[Blockchain.Command[_]],
    miner: ActorRef[Miner.Command],
    broker: ActorRef[Broker.Command]
) extends AbstractBehavior[Node.Command](context)
    with Loggable {
  private val duration                                              = 10.seconds
  implicit val timeout: Timeout                                     = Timeout(duration)
  implicit val system: ActorSystem[Nothing]                         = context.system
  implicit val ec                                                   = system.executionContext
  override def onMessage(msg: Node.Command): Behavior[Node.Command] = msg match {

    case Node.AddTransaction(transaction, replyTo) =>
      val futureAdd = broker.ask(Broker.AddTransaction(transaction, _))
      futureAdd.onComplete {
        case Failure(exception)   =>
          replyTo ! StatusReply.error(exception)
        case Success(statusReply) =>
          replyTo ! statusReply
      }
      Behaviors.same

    case Node.CheckPowSolution(solution, replyTo) =>
      val futureStr = blockchain.askWithStatus(Blockchain.GetLastHash)
      val lashHash  = Await.result(futureStr, duration)
      if (PoWUtil.validProof(lashHash, solution)) {
        replyTo ! StatusReply.Ack
        Behaviors.same
      } else {
        replyTo ! StatusReply.error("invalid PoW solution")
        Behaviors.same
      }
    case Node.AddBlock(proof, replyTo)            =>
      val futureStr = blockchain.askWithStatus(Blockchain.GetLastHash)
      val lashHash  = Await.result(futureStr, duration)
      if (PoWUtil.validProof(lashHash, proof)) {
        broker.askWithStatus(Broker.GetTransactions).onComplete {
          case Failure(exception)    =>
            replyTo ! StatusReply.error(exception)
          case Success(transactions) =>
            blockchain.ask(Blockchain.AddBlock(transactions, proof, _)).onComplete {
              case Failure(exception)   =>
                replyTo ! StatusReply.error(exception)
              case Success(statusReply) =>
                replyTo ! statusReply
            }
        }
      } else {
        replyTo ! StatusReply.error("invalid PoW solution")
      }
      Behaviors.same
    case Node.GetTransactions(replyTo)            =>
      broker.ask(Broker.GetTransactions).onComplete {
        case Failure(exception)   =>
          replyTo ! StatusReply.error(exception)
        case Success(statusReply) =>
          replyTo ! statusReply
      }
      Behaviors.same
    case Node.StartMining                         =>
      logger.info("Asking miner...")
      blockchain.askWithStatus(Blockchain.GetLastHash).onComplete {
        case Failure(exception) =>
          throw exception
        case Success(hash)      =>
          miner.askWithStatus(Miner.Mine(hash, _)).onComplete {
            case Failure(exception) =>
              throw exception
            case Success(mined)     =>
              logger.info(s"PoW:$mined")
          }
      }
      Behaviors.same
    case Node.GetLastBlockIndex(replyTo)          =>
      blockchain.ask(Blockchain.GetLastIndex).onComplete {
        case Failure(exception)   =>
          replyTo ! StatusReply.error(exception)
        case Success(statusReply) =>
          replyTo ! statusReply
      }
      Behaviors.same
    case Node.GetLastBlockHash(replyTo)           =>
      blockchain.ask(Blockchain.GetLastHash).onComplete {
        case Failure(exception)   =>
          replyTo ! StatusReply.error(exception)
        case Success(statusReply) =>
          replyTo ! statusReply
      }
      Behaviors.same
  }

}
