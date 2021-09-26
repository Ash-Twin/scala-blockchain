package me.james.chain.actor

import akka.pattern.StatusReply
import me.james.chain.model.Transaction

object Node {
  sealed trait Command
  case class AddTransaction(transaction: Transaction, replyTo: StatusReply[_]) extends Command
  case class CheckPowSolution(solution: Long, replyTo: StatusReply[_])         extends Command
  case class AddBlock(proof: Long, replyTo: StatusReply[_])                    extends Command
  case class GetTransactions(replyTo: StatusReply[_])                          extends Command
  case class Mine(replyTo: StatusReply[_])                                     extends Command
  case class StopMining(replyTo: StatusReply[_])                               extends Command
  case class GetStatus(replyTo: StatusReply[_])                                extends Command
  case class GetLastBlockIndex(replyTo: StatusReply[_])                        extends Command
  case class GetLastBlockHash(replyTo: StatusReply[_])                         extends Command
}
