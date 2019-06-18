package akka

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ InitialStateAsEvents, MemberExited, MemberRemoved, MemberUp }
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish, Subscribe, SubscribeAck }
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

case class GetTreeJson()
case class RegisterActor(actorId: String, parentId: String, actorName: String, actorValue: String)
case class UnregisterActor(actorId: String)
case class RegisterActorCluster(actorId: String, parentId: String, actorName: String, actorValue: String, node: String)
case class UnregisterActorCluster(actorId: String, node: String)

case class GetNodeUpdate()
case class NodeUpdate(node: Tree)

object TreeModelActor {
  def props(clusterFlag: Boolean): Props = Props(new TreeModelActor(clusterFlag))
}

class TreeModelActor(clusterFlag: Boolean) extends Actor with ActorLogging {
  implicit val ec = context.system.dispatcher
  var tree: Tree = null
  var mediator: ActorRef = null
  var cluster: Cluster = null

  override def preStart(): Unit = {
    if (clusterFlag) {
      cluster = Cluster(context.system)
      tree = Tree("cluster", "cluster", "cluster", 0, "", List.empty[Tree], "")
      tree = Tree.updateNode(
        tree,
        Tree(cluster.selfAddress.toString, cluster.selfAddress.toString, "member", 0, cluster.selfAddress.toString, List.empty[Tree], ""))

      mediator = DistributedPubSub(context.system).mediator
      mediator ! Subscribe("cluster-vis", self)

      context.system.scheduler.schedule(Duration(0, duration.SECONDS), Duration(5, duration.SECONDS), self, "cluster_update")
      cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberExited], classOf[MemberUp])
    } else {
      tree = Tree("user", "user", "cluster", 0, "", List.empty[Tree], "")
    }
  }

  override def receive: Receive = {
    case GetTreeJson => {
      sender() ! Tree.toJson(tree)
    }
    case r: RegisterActor => {
      println("Register Actor: " + r.toString)
      //      mediator ! Publish("cluster-vis", RegisterActorCluster(r.actorId, r.parentId, r.actorName, r.parentId, cluster.selfAddress.toString))
      if (clusterFlag)
        if (r.parentId.equals("user"))
          tree = Tree.addActor(tree, r.actorId, cluster.selfAddress.toString, r.actorName, r.actorValue, cluster.selfAddress.toString)
        else
          tree = Tree.addActor(tree, r.actorId, r.parentId, r.actorName, r.actorValue, cluster.selfAddress.toString)
      else
        tree = Tree.addActor(tree, r.actorId, r.parentId, r.actorName, r.actorValue, null)
    }
    case r: RegisterActorCluster => {
      if (r.node != cluster.selfAddress.toString) {
        println("Register Cluster Actor: " + r.toString)
        if (r.parentId.equals("user") && clusterFlag)
          tree = Tree.addActor(tree, r.actorId, r.node, r.actorName, cluster.selfAddress.toString, r.actorValue)
        else
          tree = Tree.addActor(tree, r.actorId, r.parentId, r.actorName, cluster.selfAddress.toString, r.actorValue)
      }
    }
    case r: UnregisterActor => {
      println("Unregister Actor: " + r.toString)
      //      mediator ! Publish("cluster-vis", UnregisterActorCluster(r.actorId, cluster.selfAddress.toString))
      tree = Tree.removeActor(tree, r.actorId)
    }
    //    case r: UnregisterActorCluster => {
    //      if (r.node != cluster.selfAddress.toString) {
    //        println("Unregister Cluster Actor: " + r.toString)
    //        tree = Tree.removeActor(tree, r.actorId)
    //      }
    //    }
    case GetNodeUpdate =>
      log.info("Received: Cluster Update Publish")
      mediator ! Publish("cluster-vis", NodeUpdate(Tree.getNode(tree, cluster.selfAddress.toString)))
    case n: NodeUpdate =>
      log.info("Received: Node Update")
      tree = Tree.updateNode(tree, n.node)
    case m: MemberExited =>
      log.info("Member removed: " + m.member.address.toString)
      tree = Tree.removeNode(tree, m.member.address.toString)
    case "cluster_update" =>
      log.info("Cluster Update: ")
      mediator ! Publish("cluster-vis", GetNodeUpdate)
    case sn: StopNode => {
      if (cluster.selfAddress.toString.equals(sn.nodeUrl))
        System.exit(1)
      else {
        log.info("Member removed: " + sn.nodeUrl)
        tree = Tree.removeNode(tree, sn.nodeUrl)

        mediator ! Publish("cluster-vis", sn)
      }
    }
    case m: Any => log.info("Received Unknown Message: " + m.toString + " From:" + sender().path.address)

  }
}

case class Tree(name: String, id: String, nodeType: String, events: Int, node: String, children: List[Tree], value: String)
object Tree {
  def toJson(tree: Tree): String = {

    implicit val formats = DefaultFormats

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
