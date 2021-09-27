package me.james.chain.module

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import com.google.inject.{AbstractModule, Provides, Singleton}
import me.james.chain.actor.{Blockchain, Broker, Miner, Node}
import me.james.chain.config.AppConfig
import net.codingwell.scalaguice.ScalaModule
import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class ActorModule(actorContext: ActorContext[_]) extends AbstractModule with ScalaModule {

  override def configure(): Unit =
    bind[ActorContext[_]].toInstance(actorContext)

  @Provides
  @Singleton
  def config: AppConfig = ConfigSource.file("application.conf").loadOrThrow[AppConfig]

  @Provides
  @Singleton
  def minerActor(actorContext: ActorContext[_]): ActorRef[Miner.Command] = actorContext.spawn(Miner.apply(), "Miner")

  @Provides
  @Singleton
  def brokerActor(actorContext: ActorContext[_]): ActorRef[Broker.Command] =
    actorContext.spawn(Broker.apply(), "Broker")

  @Provides
  @Singleton
  def blockChain(actorContext: ActorContext[_]): ActorRef[Blockchain.Command[_]] =
    actorContext.spawn(Blockchain.apply(config), "BlockChain")

  @Provides
  @Singleton
  def nodeActor(
      actorContext: ActorContext[_],
      miner: ActorRef[Miner.Command],
      broker: ActorRef[Broker.Command],
      blockchain: ActorRef[Blockchain.Command[_]]
  ): ActorRef[Node.Command] = actorContext.spawn(Node.apply(blockchain, miner, broker), "Node")
}
