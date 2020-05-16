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
  var ticker: Cancellable = _
  implicit val system = context.system
  implicit val ec = system.dispatcher

  override def preStart(): Unit = {
    TreeModelActor ! RegisterActor("Main Actor", "user", self.path.name, "Main Actor", "shard")
  }

  override def receive: Receive = {
    case GO =>
      scheduleMessageRateTicker()
    case "tick" =>
      //      println("tick")

      while (context.children.size < 5)
        context.actorOf(SecondActor.props(TreeModelActor, live), "SecondActor" + UUID.randomUUID().toString)
  }

  override def postStop(): Unit = {
    TreeModelActor ! UnregisterActor(self.path.name)
  }

  private def scheduleMessageRateTicker(): Unit = {
    //    val memberCount = cluster.state.members.size
    val messageRatePerSec = 1
    val millsPerMessage = 1000 / messageRatePerSec
    //    log.info("Message interval {}ms, cluster member count {}, {}", millsPerMessage, memberCount, event)
    val tickInterval = Duration(millsPerMessage, duration.MILLISECONDS)
    if (ticker != null) ticker.cancel
    ticker = context.system.scheduler
      .scheduleAtFixedRate(Duration(0, duration.MILLISECONDS), tickInterval, self, "tick")
  }
}
