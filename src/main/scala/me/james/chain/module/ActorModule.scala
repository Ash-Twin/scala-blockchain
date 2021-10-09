package me.james.chain.module

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
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
  def minerActor(actorContext: ActorContext[_]): ActorRef[Miner.Command] = actorContext.spawn(Miner.apply(), "Miner")

  @Provides
  @Singleton
  def brokerActor(actorContext: ActorContext[_]): ActorRef[Broker.Command] =
    actorContext.spawn(Broker.apply(), "Broker")

  @Provides
  @Singleton
  def blockChain(actorContext: ActorContext[_]): ActorRef[Blockchain.Command[_]] =
    actorContext.spawn(Blockchain.apply(appConfig, system), "BlockChain")

  @Provides
  @Singleton
  def appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]


  @Provides
  @Singleton
  def system: ActorSystem[_] = actorContext.system

  @Provides
  @Singleton
  def nodeActor(
      actorContext: ActorContext[_],
      miner: ActorRef[Miner.Command],
      broker: ActorRef[Broker.Command],
      blockchain: ActorRef[Blockchain.Command[_]]
  ): ActorRef[Node.Command] = actorContext.spawn(Node.apply(blockchain, miner, broker), "Node")
}
