package me.james.chain

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.google.inject.{Guice, Inject}
import me.james.chain.api.TransactionApi
import me.james.chain.config.AppConfig
import me.james.chain.module.ActorModule
import me.james.chain.utils.Loggable
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ChainServer extends Loggable {
  def main(args: Array[String]): Unit =
    ActorSystem(
      Behaviors.setup[Done] { ctx =>
        implicit val ec = ctx.executionContext
        val injector    = Guice.createInjector(ActorModule(ctx))
        injector.instance[ChainServer].start()
        Behaviors.receiveMessage { case Done => Behaviors.stopped }
      },
      "Chain-Server",
      ConfigSource.default.config() match {
        case Left(loadError) => throw new RuntimeException(loadError.toList.mkString("\n"))
        case Right(config)   => config
      }
    )

}
class ChainServer @Inject() (injector: ScalaInjector) extends Loggable {
  implicit val system: ActorSystem[_]        = injector.instance[ActorSystem[_]]
  implicit val ec: ExecutionContext          = system.executionContext
  private val transactionApi: TransactionApi = injector.instance[TransactionApi]
  private val appConfig: AppConfig           = injector.instance[AppConfig]
  private val HOST: String                   = appConfig.host
  private val PORT: Int                      = appConfig.port
  def start(): Unit = {
    logger.info(s"Blockchain Server initializing")
    Http()
      .newServerAt(HOST, PORT)
      .bind(transactionApi.routes)
      .andThen { active =>
        logger.info(s"Blockchain Server serving at https://$HOST:$PORT")
        active
      }
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))
  }
}
