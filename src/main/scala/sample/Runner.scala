package sample

import java.util.UUID

import akka.HttpServerActor
import akka.actor.{ ActorSystem, Props }
import akka.stream.ActorMaterializer

object Runner {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  val httpServer = system.actorOf(HttpServerActor.props(false), "http-server")

  def main(args: Array[String]): Unit = {
    println("Runner Strarting up")

    val mainActor = system.actorOf(MainActor.props(httpServer, true), "main-actor" + UUID.randomUUID().toString)
    mainActor ! GO

  }

}
