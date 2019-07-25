package akka

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ InitialStateAsEvents, MemberExited, MemberRemoved, MemberUp }
import akka.cluster.ddata.{ DistributedData, LWWMap, LWWMapKey }
import akka.cluster.ddata.Replicator.{ Delete, Get, GetSuccess, ReadAll, ReadLocal, ReadMajority, Subscribe, Update, WriteAll, WriteLocal, WriteMajority }
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

case object GetTreeJson
case class RegisterActor(actorId: String, parentId: String, actorName: String, actorValue: String)
case class UnregisterActor(actorId: String)
case class RegisterActorCluster(actorId: String, parentId: String, actorName: String, actorValue: String, node: String)
case class UnregisterActorCluster(actorId: String, node: String)

case class GetNodeUpdate()
case class NodeUpdate(node: Tree)

case class ClusterStopNode(node: String)

object TreeModelActor {
  def props(clusterFlag: Boolean, startHttp: Boolean): Props = Props(new TreeModelActor(clusterFlag, startHttp))
}

class TreeModelActor(clusterFlag: Boolean, startHttp: Boolean) extends Actor with ActorLogging {
  implicit val ec = context.system.dispatcher
  var cluster: Cluster = Cluster(context.system)

  val replicator = DistributedData(context.system).replicator
  implicit val node = DistributedData(context.system).selfUniqueAddress
  val nodeName = UUID.randomUUID().toString

  val ClusterTreeKey = LWWMapKey[String, Tree]("tree")

  var clusterVis: LWWMap[String, Tree] = LWWMap.empty[String, Tree]

  private val timeout = 100.seconds
  private val readStrategy = ReadAll(timeout)
  private val writeStrategy = WriteAll(timeout)

  var localTree = Tree(nodeName, nodeName, "member", 0,
    nodeName, List.empty[Tree], "")

  override def preStart(): Unit = {
    //    replicator ! Subscribe(ClusterTreeKey, self)

    replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))

    if (startHttp)
      context.system.actorOf(HttpServerActor.props(true, "localhost", 8080, self), "http-server")
  }

  override def receive: Receive = {
    case g @ GetTreeJson => {
      replicator ! Get(ClusterTreeKey, readStrategy, Some(sender()))
    }
    case g @ GetSuccess(ClusterTreeKey, Some(replyTo: ActorRef)) =>
      val data = Tree.toJson(g.get(ClusterTreeKey))
      replyTo ! data

    case r: RegisterActor => {
      println("Register Actor: " + r.toString)

      if (clusterFlag)
        if (r.parentId.equals("user"))
          localTree = Tree.addActor(localTree, r.actorId, nodeName, r.actorName, r.actorValue, nodeName)
        else
          localTree = Tree.addActor(localTree, r.actorId, r.parentId, r.actorName, r.actorValue, nodeName)
      else
        localTree = Tree.addActor(localTree, r.actorId, r.parentId, r.actorName, r.actorValue, null)

      replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))
    }
    case r: UnregisterActor => {
      println("Unregister Actor: " + r.toString)
      localTree = Tree.removeActor(localTree, r.actorId)

      replicator ! Update(ClusterTreeKey, LWWMap.empty[String, Tree], writeStrategy)(_ :+ (nodeName, localTree))
    }

    //    case r: UnregisterActorCluster => {
    //      if (r.node != cluster.selfAddress.toString) {
    //        println("Unregister Cluster Actor: " + r.toString)
    //        tree = Tree.removeActor(tree, r.actorId)
    //      }
    //    }
    //    case GetNodeUpdate =>
    //      log.info("Received: Cluster Update Publish")
    //      mediator ! Publish("cluster-vis", NodeUpdate(Tree.getNode(tree, cluster.selfAddress.toString)))
    //    case n: NodeUpdate =>
    //      log.info("Received: Node Update")
    //      tree = Tree.updateNode(tree, n.node)
    //    case m: MemberExited =>
    //      log.info("Member removed: " + m.member.address.toString)
    //      tree = Tree.removeNode(tree, m.member.address.toString)
    //    case "cluster_update" =>
    //      log.info("Cluster Update: ")
    //      mediator ! Publish("cluster-vis", GetNodeUpdate)
    //    case sn: StopNode => {
    //      if (cluster.selfAddress.toString.equals(sn.nodeUrl))
    //        System.exit(1)
    //      else {
    //        log.info("Member removed: " + sn.nodeUrl)
    //        tree = Tree.removeNode(tree, sn.nodeUrl)
    //
    //        mediator ! Publish("cluster-vis", ClusterStopNode(sn.nodeUrl))
    //      }
    //    }
    //    case sn: ClusterStopNode => {
    //      if (cluster.selfAddress.toString.equals(sn.node))
    //        System.exit(1)
    //    }
    case m: Any => log.info("Received Unknown Message: " + m.toString + " From:" + sender().path.address)
  }

  //  override def postStop(): Unit = {
  //    replicator ! Delete(NodeTreeKey, WriteLocal)
  //  }
}

case class Tree(name: String, id: String, nodeType: String, events: Int, node: String, children: List[Tree], value: String)
object Tree {
  def toJson(map: LWWMap[String, Tree]): String = {

    implicit val formats = DefaultFormats

    val children: List[Tree] = map.entries.map(m => {
      m._2
    }).toList

    println("Number of nodes: " + children.size)

    val tree = Tree("cluster", "cluster", "cluster", 0, "cluster", children, "")

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

  def addActor(tree: Tree, actorId: String, parentId: String, actorName: String, actorValue: String, node: String): Tree = {
    println("tree id: " + tree.id + " parentid:" + parentId + " tree node:" + tree.node + " node:" + node)
    if (tree.id.equals(parentId) && tree.node.equals(node)) {
      val newChild = new Tree(actorName, actorId, "shard", 0, node, List.empty[Tree], actorValue)
      new Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChild :: tree.children, tree.value)
    } else {
      val newChildren: List[Tree] = tree.children.map(c => {
        addActor(c, actorId, parentId, actorName, actorValue, node)
      })

      Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChildren, tree.value)
    }

  }

  def updateNode(tree: Tree, node: Tree): Tree = {
    println("Add Node: " + node.name)
    val newChildren: List[Tree] = node :: tree.children.filter(_.id != node.id)
    Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChildren, tree.value)
  }

  def removeNode(tree: Tree, nodeAddress: String): Tree = {
    val newChildren: List[Tree] = tree.children.filter(_.id != nodeAddress)
    Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChildren, tree.value)
  }

  def removeActor(tree: Tree, actorId: String): Tree = {
    if (tree.children.filter(_.id.equals(actorId)).size > 0) {
      val newChildren = tree.children.filterNot(_.id.equals(actorId))
      Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChildren, tree.value)
    } else {
      val newChildren: List[Tree] = tree.children.map(c => {
        removeActor(c, actorId)
      })

      Tree(tree.name, tree.id, tree.nodeType, tree.events, tree.node, newChildren, tree.value)
    }
  }
}
