package sample

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import scala.concurrent.duration._

case object GO

object MainActor {
  def props(shardRegion: ActorRef): Props = Props(new MainActor(shardRegion))
}

class MainActor(shardRegion: ActorRef) extends Actor with ActorLogging {
  var ticker: Cancellable = null
  implicit val system = context.system
  implicit val ec = system.dispatcher

  override def preStart(): Unit = {
    context.system.scheduler.schedule(5 seconds, 5 seconds, self, "tick")
  }

  override def receive: Receive = {
    case "tick" => {
      println("tick")

      shardRegion ! TestMessage(UUID.randomUUID().toString)
    }
  }
}
