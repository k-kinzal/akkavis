package sample

import java.util.UUID

import akka.TreeModelActor
import akka.actor.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.ConfigFactory

object ClusterRunner {
  implicit val config = ConfigFactory.load()
  implicit val system = ActorSystem("my-system")
  implicit val executionContext = system.dispatcher
  val treeActor = system.actorOf(TreeModelActor.props(startHttp = true, config.getString("akkavis.hostname"), config.getInt("akkavis.port")), "tree-actor")

  def main(args: Array[String]): Unit = {

    // Akka Management hosts the HTTP routes used by bootstrap
    AkkaManagement(system).start()

    // Starting the bootstrap process needs to be done explicitly
    ClusterBootstrap(system).start()
    //    println("Runner Strarting up")

    val mainActor = system.actorOf(MainActor.props(treeActor, live = true), "main-actor" + UUID.randomUUID().toString)
    mainActor ! GO

  }

}
