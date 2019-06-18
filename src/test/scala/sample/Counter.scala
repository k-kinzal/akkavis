package sample

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}

import scala.concurrent.duration._

case object Increment
case object Decrement
final case class Get(counterId: Long)
final case class EntityEnvelope(id: Long, payload: Any)

case object Stop
final case class CounterChanged(delta: Int)

class Counter extends Actor {

  override def preStart {
    println("Counter Actor Started")
  }

  var count = 0

  def updateState(delta: Int): Unit =
    count += delta

  override def receive: Receive = {
    case Increment      => updateState(+1)
    case Decrement      => updateState(-1)
    case Get(_)         => {
      println("Count: "+count)
      sender() ! count
    }
//    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop           => context.stop(self)
  }
}