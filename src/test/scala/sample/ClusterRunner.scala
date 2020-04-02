package sample

import java.util.UUID

import akka.TreeModelActor
import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer

object ClusterRunner {
  implicit val system = ActorSystem("AkkaClusterVis")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {

    //    // Akka Management hosts the HTTP routes used by bootstrap
    //    AkkaManagement(system).start()

    //    // Starting the bootstrap process needs to be done explicitly
    //    ClusterBootstrap(system).start()
    println("Runner Strarting up")

    val shardRegion: ActorRef = ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = Props[ShardActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = ShardActor.extractEntityId,
      extractShardId = ShardActor.extractShardId)

    system.actorOf(TreeModelActor.props(true, 8080, shardRegion))

    val mainActor = system.actorOf(MainActor.props(shardRegion), "main-actor" + UUID.randomUUID().toString)

  }

}
