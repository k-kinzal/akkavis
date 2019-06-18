//package sample
//
//import akka.HttpServerActor
//import akka.actor.{ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit}
//import akka.util.Timeout
//import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
//
//import scala.concurrent.duration._
//
//class InventoryServerSpec extends TestKit(ActorSystem("my-system"))
//  with FlatSpecLike with ImplicitSender with Matchers with BeforeAndAfterAll {
//
//  //  val config = ConfigFactory.load()
//  implicit val timeout: Timeout = 3.seconds
//
//  override def beforeAll(): Unit = {
//    super.beforeAll()
//    system.log.info("Starting in TEST")
//  }
//
//  override def afterAll(): Unit = {
//    TestKit.shutdownActorSystem(system)
//  }
//
//
//  "A Product Manager" must "be automatically started" in {
//    val httpServer = system.actorOf(Props[HttpServerActor], "http-server")
//
//    println("System Strarting up")
//
//    val mainActor = system.actorOf(MainActor.props(httpServer, false), "main-actor")
//    mainActor ! GO
//
//
//
//  }
//}
