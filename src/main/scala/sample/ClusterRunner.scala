package sample

import java.util.UUID

import akka.{ HttpServerActor, TreeModelActor }
import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer

object ClusterRunner {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  //  val treeActor = system.actorOf(TreeModelActor.props(true, true), "tree-actor")

  def main(args: Array[String]): Unit = {

    // Akka Management hosts the HTTP routes used by bootstrap
    AkkaManagement(system).start()

    // Starting the bootstrap process needs to be done explicitly
    ClusterBootstrap(system).start()
    println("Runner Strarting up")

    //    val mainActor = system.actorOf(MainActor.props(treeActor, true), "main-actor" + UUID.randomUUID().toString)
    //    mainActor ! GO

  }

}
