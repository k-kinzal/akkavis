package sample

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akkavisualizer.TreeModelActor
import com.typesafe.config.ConfigFactory

object ClusterRunner {

  def main(args: Array[String]): Unit = {

    def isInt(s: String): Boolean = s.matches("""\d+""")

    args.toList match {
      case portString :: frontEndPort :: Nil if isInt(portString) && isInt(frontEndPort) =>
        init(portString.toInt, frontEndPort.toInt)
      case _ =>
        throw new IllegalArgumentException("usage: <remotingPort> <frontEndPort>")
    }

  }

  def init(remotePort: Int, frontEndPort: Int) = {
    val config = ConfigFactory.load()
    val clusterConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port = " + remotePort)

    implicit val system = ActorSystem("AkkaClusterVis", clusterConfig.withFallback(config))
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    println("Runner Strarting up")

    val shardRegion: ActorRef = ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = Props[ShardActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = ShardActor.extractEntityId,
      extractShardId = ShardActor.extractShardId)

    system.actorOf(TreeModelActor.props(frontEndPort, shardRegion))

    val mainActor = system.actorOf(MainActor.props(shardRegion), "main-actor" + UUID.randomUUID().toString)

  }

}
