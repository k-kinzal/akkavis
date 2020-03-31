//package sample.cluster.stats
//
//import java.util.UUID
//
//import akka.{GetTreeJson, HttpServerActor, TreeModelActor}
//
//import language.postfixOps
//import scala.concurrent.duration._
//import akka.actor.Props
//import akka.actor.RootActorPath
//import akka.cluster.Cluster
//import akka.cluster.Member
//import akka.cluster.MemberStatus
//import akka.cluster.ClusterEvent.CurrentClusterState
//import akka.cluster.ClusterEvent.MemberUp
//import akka.cluster.ddata.DistributedData
//import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
//import akka.remote.testkit.MultiNodeConfig
//import com.typesafe.config.ConfigFactory
//import sample.{GO, MainActor}
//import akka.pattern.{ask, pipe}
//import akka.remote.testconductor.RoleName
//import akka.util.Timeout
//
//import scala.concurrent.{ExecutionContext, Future}
//
//object StatsSampleSpecConfig extends MultiNodeConfig {
//  // register the named roles (nodes) of the test
//  val node1 = role("node1")
//  val node2 = role("node2")
//  val node3 = role("node3")
//
//  def nodeList = Seq(node1,node2,node3)
//
//  commonConfig(ConfigFactory.parseString("""
//    akka.loglevel = INFO
//    akka.actor.provider = "cluster"
//    akka.log-dead-letters-during-shutdown = off
//    """))
//
//}
//// need one concrete test class per node
//class StatsSampleSpecMultiJvmNode1 extends StatsSampleSpec
//class StatsSampleSpecMultiJvmNode2 extends StatsSampleSpec
//class StatsSampleSpecMultiJvmNode3 extends StatsSampleSpec
//
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.WordSpecLike
//import org.scalatest.Matchers
//import akka.remote.testkit.MultiNodeSpec
//import akka.testkit.ImplicitSender
//
//abstract class StatsSampleSpec extends MultiNodeSpec(StatsSampleSpecConfig)
//  with WordSpecLike with Matchers with BeforeAndAfterAll
//  with ImplicitSender {
//
//  import StatsSampleSpecConfig._
//
//  override def initialParticipants = roles.size
//
//  override def beforeAll() = multiNodeSpecBeforeAll()
//
//  override def afterAll() = multiNodeSpecAfterAll()
//
//  val cluster = Cluster(system)
//
//  def join(from: RoleName, to: RoleName): Unit = {
//    runOn(from) {
//      cluster join node(to).address
//    }
//    enterBarrier(from.name + "-joined")
//  }
//
//  "CLuster Visualizer" must {
//
//    "illustrate how to startup cluster" in within(15 seconds) {
//      join(node1, node1)
//      join(node2, node1)
//      join(node3, node1)
//
//      awaitAssert {
//        DistributedData(system).replicator ! GetReplicaCount
//        expectMsg(ReplicaCount(roles.size))
//      }
//      enterBarrier("after-1")
//    }
//
//    "Get Cluster Nodes" in within(15 seconds) {
//      runOn(node1) {
//        implicit val timeout = Timeout(15 seconds) // needed for `?` below
//        implicit val ec = ExecutionContext.global
//
//        val treeactor = system.actorOf(TreeModelActor.props(true, true), "tree-actor")
//
//        awaitAssert {
//          treeactor ! GetTreeJson
//          val msg = expectMsgType[String]
//          msg should not be empty
//        }
//      }
//
//
////      runOn(second) {
////        system.actorOf(TreeModelActor.props(true, false), "tree-actor")
////      }
////
////      runOn(third) {
////        system.actorOf(TreeModelActor.props(true, false), "tree-actor")
////      }
//    }
////
////    "remove an actor" in within(15 seconds) {
////
////    }
////
////    "remove a node" in within(15 seconds) {
////      runOn(second) {
////
////      }
////    }
//
//  }
//
//}
