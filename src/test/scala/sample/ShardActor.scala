package sample

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.cluster.sharding.ShardRegion
import akka.{ RegisterActor, UnregisterActor }

import scala.concurrent.duration
import scala.concurrent.duration.Duration

case class TestMessage(id: String)

object ShardActor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg: TestMessage ⇒ (msg.id, msg)
  }

  private val numberOfShards = 5

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg: TestMessage ⇒ (Math.abs(msg.id.hashCode) % numberOfShards).toString
  }

}

class ShardActor extends Actor with ActorLogging {
  var ticker: Cancellable = null
  implicit val system = context.system
  implicit val ec = system.dispatcher

  override def receive: Receive = {
    case "kill" => context.stop(self)
  }
}
