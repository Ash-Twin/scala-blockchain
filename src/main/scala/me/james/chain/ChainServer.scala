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

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ChainServer extends Loggable {
  def main(args: Array[String]): Unit =
    ActorSystem(
      Behaviors.setup[Done] { ctx =>
        implicit val ec = ctx.executionContext
        val injector    = Guice.createInjector(ActorModule(ctx))
        injector.instance[ChainServer].start() onComplete {
          case Success(_)     =>
            logger.info("binding future complete")
          case Failure(exception) =>
            ctx.system.terminate()
        }
        Behaviors.receiveMessage { case Done => Behaviors.stopped }
      },
      "Chain-Server",
      ConfigSource.default.config() match {
        case Left(loadError)  => throw new RuntimeException(loadError.toList.mkString("\n"))
        case Right(config) => config
      }
    )

}
class ChainServer @Inject() (injector: ScalaInjector) {
  private implicit val system: ActorSystem[_] = injector.instance[ActorSystem[_]]
  val tApi = injector.instance[TransactionApi]
  private val appConfig: AppConfig            = injector.instance[AppConfig]

  def start(): Future[Http.ServerBinding] =
    Http().newServerAt(appConfig.host, appConfig.port).bind(tApi.routes)
}
