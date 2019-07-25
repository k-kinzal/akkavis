package sample

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.{ RegisterActor, UnregisterActor }

import scala.concurrent.duration
import scala.concurrent.duration.Duration

case object GO

object MainActor {
  def props(TreeModelActor: ActorRef, live: Boolean): Props = Props(new MainActor(TreeModelActor, live))
}

class MainActor(TreeModelActor: ActorRef, live: Boolean) extends Actor with ActorLogging {
  var ticker: Cancellable = null
  implicit val system = context.system
  implicit val ec = system.dispatcher
  var master = false

  override def preStart(): Unit = {

    TreeModelActor ! RegisterActor(self.path.name, context.parent.path.name, self.path.name, Math.random().toString)

    if (live)
      context.system.scheduler.scheduleOnce(Duration(5, duration.SECONDS), self, "kill")

  }

  override def receive: Receive = {
    case GO => {
      master = true
      scheduleMessageRateTicker
    }
    case "tick" => {
      println("tick")
      if (context.children.size < 5)
        context.actorOf(MainActor.props(TreeModelActor, live), "MainActor" + UUID.randomUUID().toString)
    }
    case "kill" => {
      if (!master)
        context.stop(self)
    }

  }

  override def postStop(): Unit = {
    TreeModelActor ! UnregisterActor(self.path.name)
  }

  private def scheduleMessageRateTicker: Unit = {
    //    val memberCount = cluster.state.members.size
    val messageRatePerSec = 1
    val millsPerMessage = 1000 / messageRatePerSec
    //    log.info("Message interval {}ms, cluster member count {}, {}", millsPerMessage, memberCount, event)
    val tickInterval = Duration(millsPerMessage, duration.MILLISECONDS)
    if (ticker != null) ticker.cancel
    ticker = context.system.scheduler
      .schedule(Duration(0, duration.MILLISECONDS), tickInterval, self, "tick")
  }
}
