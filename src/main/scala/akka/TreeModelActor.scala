package akka

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.Cluster
import akka.cluster.ddata.{ DistributedData, LWWMap, LWWMapKey }
import akka.cluster.ddata.Replicator.{ Get, GetSuccess, ReadAll, Update, WriteAll }
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ CurrentShardRegionState, GetShardRegionState }
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

case object GetTreeJson
case class RegisterActor(actorId: String, parentId: String, actorName: String, actorValue: String, nodeType: String)
case class UnregisterActor(actorId: String)
case class RegisterActorCluster(actorId: String, parentId: String, actorName: String, actorValue: String, node: String)
case class UnregisterActorCluster(actorId: String, node: String)

case class GetNodeUpdate()
case class UpdateTree()
case class NodeUpdate(node: Tree)

case class ClusterStopNode(node: String)

object TreeModelActor {
  def props(startHttp: Boolean, port: Int, shardRegion: ActorRef): Props = Props(new TreeModelActor(startHttp, port, shardRegion))
}

class TreeModelActor(startHttp: Boolean, port: Int, shardRegion: ActorRef) extends Actor with ActorLogging {
  implicit val ec = context.system.dispatcher
  var cluster: Cluster = Cluster(context.system)

  val replicator = DistributedData(context.system).replicator
  implicit val node = DistributedData(context.system).selfUniqueAddress
  val nodeName = cluster.selfAddress.toString

  val ClusterTreeKey = LWWMapKey[String, Tree]("tree")

  var clusterVis: LWWMap[String, Tree] = LWWMap.empty[String, Tree]

  private val timeout = 100.seconds
  private val readStrategy = ReadAll(timeout)
  private val writeStrategy = WriteAll(timeout)

  var localTree = Tree(nodeName, nodeName, "member", 0, List.empty[Tree], "")

  val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit = {
    log.info("Starting Tree Actor")

    replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))

    mediator ! Subscribe("cluster-node-killswitch", self)

    //    context.system.scheduler.schedule(5.seconds, 5.seconds, self, UpdateTree)
    //    context.system.scheduler..scheduleWithFixedDelay(Duration.Zero, 50.milliseconds, tickActor, Tick)
    context.system.scheduler.schedule(0 milliseconds, 5 seconds, self, UpdateTree)

    if (startHttp)
      context.system.actorOf(HttpServerActor.props(true, "localhost", port, self), "http-server")
  }

  override def receive: Receive = {
    case g @ GetTreeJson => {
      replicator ! Get(ClusterTreeKey, readStrategy, Some(sender()))
    }
    case g @ GetSuccess(ClusterTreeKey, Some(replyTo: ActorRef)) =>
      val data = Tree.toJson(g.get(ClusterTreeKey))
      replyTo ! data
    case UpdateTree =>
      log.info("Update Tree Message Received")
      localTree = Tree(nodeName, nodeName, "member", 0, List.empty[Tree], "")

      shardRegion ! GetShardRegionState
    case sr: CurrentShardRegionState =>
      log.info("CurrentShardRegionState Message Received")

      sr.shards.map(shard => {
        log.info("Add Shard: " + shard.shardId)
        self ! RegisterActor("Shard-" + shard.shardId + "", "", "Shard-" + shard.shardId + "", "", "shard")

        shard.entityIds.map(id => {
          log.info("Add Shard: " + shard.shardId + " Entity: " + id)
          self ! RegisterActor(id, "Shard-" + shard.shardId, id, id, "entity")
        })
      })
    case r: RegisterActor => {
      println("Register Actor: " + r.toString)

      if (r.parentId.equals("user") || r.nodeType.equals("shard"))
        localTree = Tree.addActor(localTree, r.actorId, nodeName, r.actorName, r.actorValue, r.nodeType)
      else
        localTree = Tree.addActor(localTree, r.actorId, r.parentId, r.actorName, r.actorValue, r.nodeType)

      replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))
    }
    //    case r: UnregisterActor => {
    //      println("Unregister Actor: " + r.toString)
    //      localTree = Tree.removeActor(localTree, r.actorId)
    //
    //      replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))
    //    }
    case sn: StopNode => {
      if (cluster.selfAddress.toString.equals(sn.nodeUrl)) {
        replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_.remove(node, nodeName))

        System.exit(1)
      } else {
        mediator ! Publish("cluster-node-killswitch", ClusterStopNode(sn.nodeUrl))
      }
    }
    case sn: ClusterStopNode => {
      if (cluster.selfAddress.toString.equals(sn.node)) {
        replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_.remove(node, nodeName))

        System.exit(1)
      }
    }
    case m: Any => log.info("Received Unknown Message: " + m.toString + " From:" + sender().path.address)
  }
}

case class Tree(name: String, id: String, nodeType: String, events: Int, children: List[Tree], value: String)
object Tree {
  def toJson(map: LWWMap[String, Tree]): String = {

    implicit val formats = DefaultFormats

    val children: List[Tree] = map.entries.map(m => {
      m._2
    }).toList

    println("Number of nodes: " + children.size)

    val tree = Tree("cluster", "cluster", "cluster", 0, children, "")

    val jsonString = write(tree)
    println(jsonString)
    jsonString
  }

  def getNode(tree: Tree, nodeAddress: String): Tree = {
    tree.children.foreach(c => {
      if (c.id.equals(nodeAddress))
        return c
    })

    null
  }

  def addActor(tree: Tree, actorId: String, parentId: String, actorName: String, actorValue: String, nodeType: String): Tree = {
    println("tree id: " + tree.id + " parentName:" + parentId)
    if (tree.id.equals(parentId)) {
      val newChild = new Tree(actorName, actorId, nodeType, 0, List.empty[Tree], actorValue)
      new Tree(tree.name, tree.id, tree.nodeType, tree.events, newChild :: tree.children, tree.value)
    } else {
      val newChildren: List[Tree] = tree.children.map(c => {
        addActor(c, actorId, parentId, actorName, actorValue, nodeType)
      })

      Tree(tree.name, tree.id, tree.nodeType, tree.events, newChildren, tree.value)
    }

  }

  def updateNode(tree: Tree, node: Tree): Tree = {
    println("Add Node: " + node.name)
    val newChildren: List[Tree] = node :: tree.children.filter(_.id != node.id)
    Tree(tree.name, tree.id, tree.nodeType, tree.events, newChildren, tree.value)
  }

  def removeNode(tree: Tree, nodeAddress: String): Tree = {
    val newChildren: List[Tree] = tree.children.filter(_.id != nodeAddress)
    Tree(tree.name, tree.id, tree.nodeType, tree.events, newChildren, tree.value)
  }

  def removeActor(tree: Tree, actorId: String): Tree = {
    if (tree.children.filter(_.id.equals(actorId)).size > 0) {
      val newChildren = tree.children.filterNot(_.id.equals(actorId))
      Tree(tree.name, tree.id, tree.nodeType, tree.events, newChildren, tree.value)
    } else {
      val newChildren: List[Tree] = tree.children.map(c => {
        removeActor(c, actorId)
      })

      Tree(tree.name, tree.id, tree.nodeType, tree.events, newChildren, tree.value)
    }
  }
}
