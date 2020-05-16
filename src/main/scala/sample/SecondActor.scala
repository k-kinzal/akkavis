package sample

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.{ RegisterActor, UnregisterActor }

import scala.concurrent.duration
import scala.concurrent.duration.Duration

object SecondActor {
  def props(TreeModelActor: ActorRef, live: Boolean): Props = Props(new SecondActor(TreeModelActor, live))
}

class SecondActor(TreeModelActor: ActorRef, live: Boolean) extends Actor with ActorLogging {
  var ticker: Cancellable = _
  implicit val system = context.system
  implicit val ec = system.dispatcher

  override def preStart(): Unit = {
    TreeModelActor ! RegisterActor(self.path.name, "Main Actor", self.path.name, "", "entity")

    context.system.scheduler.scheduleOnce(Duration(5, duration.SECONDS), self, "kill")
  }

  override def receive: Receive = {
    case "kill" => context.stop(self)
  }

  override def postStop(): Unit = {
    TreeModelActor ! UnregisterActor(self.path.name)
  }
}
