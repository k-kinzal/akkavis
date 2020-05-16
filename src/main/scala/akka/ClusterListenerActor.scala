package akka

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

class SimpleClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberJoined], classOf[MemberRemoved])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.debug("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.debug("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.debug("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
}