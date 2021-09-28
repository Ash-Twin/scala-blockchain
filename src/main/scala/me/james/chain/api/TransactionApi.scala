package me.james.chain.api

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import io.circe.generic.auto._
import io.circe.syntax._
import me.james.chain.actor.Node
import me.james.chain.model.Transaction
import me.james.chain.utils.JsonSupport
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector

import scala.concurrent.Future

class TransactionApi @Inject() (injector: ScalaInjector) extends JsonSupport {
  private val node: ActorRef[Node.Command]    = injector.instance[ActorRef[Node.Command]]
  private implicit val system: ActorSystem[_] = injector.instance[ActorSystem[_]]
  def routes: Route                           =
    path("transaction") {
      get {
        val future: Future[List[Transaction]] = node.askWithStatus(Node.GetTransactions)
        onSuccess(future) {
          case transactions: List[Transaction] => statusWithdata(StatusCodes.OK, transactions.asJson.noSpaces)
          case _                               => statusWithdata(StatusCodes.InternalServerError)
        }
      } ~ post {
        entity(as[Transaction]) { transaction =>
          val future = node.askWithStatus(Node.AddTransaction(transaction, _))
          onSuccess(future) {
            case Done => statusWithdata(StatusCodes.Created, transaction.asJson.noSpaces)
            case _    => statusWithdata(StatusCodes.InternalServerError)
          }
        }
      }
    }~
      path("mine"){
        get{
          node ! Node.StartMining
          statusWithdata(StatusCodes.OK,"start mining")
        }
      }
}
