package akka

import java.io.IOException

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage, UpgradeToWebSocket }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout

import scala.concurrent.duration
import scala.concurrent.duration._

case class EntityMessage(memberId: String, shardId: String, entityId: String, action: String, forward: Boolean)

case class ClusterMessage()

case class StopNode(nodeUrl: String)

object HttpServerActor {
  def props(host: String, port: Int, treeActor: ActorRef): Props = Props(new HttpServerActor(host, port, treeActor))
}

class HttpServerActor(host: String, port: Int, treeActor: ActorRef) extends Actor with ActorLogging {
  implicit val actorSystem = context.system
  implicit val actorMaterializer = Materializer.apply(actorSystem)
  implicit val executionContext = actorSystem.dispatcher

  override def receive: Receive = {
    case m: Any => log.debug("unknown Message:" + m)
  }

  override def preStart(): Unit = {
    log.debug("Start")
    startHttpServer()
  }

  def startHttpServer(): Unit = {
    log.debug("Attempting to Start up Cluster Visualization Http Server")

    val bindingFuture = Http().bindAndHandleSync(requestHandler, host, port)

    bindingFuture.failed.foreach { ex =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
    }

    log.debug(s"Cluster Visualization Server online at http://{}:{}/", host, port)

  }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/events"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(updateTreeWebSocketService)
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      log.debug(r.uri.path.toString())
      r.uri.path.toString() match {
        case "/" => htmlFileResponse("monitor.html")
        case "/d3.v5.js" => jsFileResponse("d3.v5.js")
        case "/monitor2" => htmlFileResponse("monitor2.html")
        case "/monitor3" => htmlFileResponse("monitor3.html")
        case "/d3.js" => jsFileResponse("d3.js")
        case "/d3.geom.js" => jsFileResponse("d3.geom.js")
        case "/d3.layout.js" => jsFileResponse("d3.layout.js")
        case _ =>
          r.discardEntityBytes()
          HttpResponse(StatusCodes.NotFound)
      }

  }

  def htmlFileResponse(filename: String): HttpResponse = {
    try {
      val fileContents: String = readFile(filename)
      HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, fileContents))
    } catch {
      case e: IOException =>
        log.error(e, String.format("I/O error on file '%s'", filename))
        HttpResponse(StatusCodes.InternalServerError)
    }
  }

  def jsFileResponse(filename: String): HttpResponse = {
    try {
      val fileContents: String = readFile(filename)

      HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`), fileContents))
    } catch {
      case e: IOException =>
        log.error(e, String.format("I/O error on file '%s'", filename))
        HttpResponse(StatusCodes.InternalServerError)
    }
  }

  def readFile(filename: String): String = {
    val fileString = scala.io.Source.fromResource(filename).mkString
    fileString
  }

  implicit val askTimeout = Timeout(5.seconds)

  val updateTreeWebSocketService =
    Flow[Message]
      .mapConcat {
        case tm: TextMessage => tm :: Nil
        case _: BinaryMessage => Nil
      }
      .map(m => {
        m.toStrict(Duration(1, duration.SECONDS)).onComplete(m => {
          log.debug("Websocket Message: " + m.get.text)
          val message = m.get.text
          log.debug("Received HTTP Message: " + message)
          if (message.contains("akka.tcp")) {
            treeActor ! StopNode(message)
          }
        })

        GetTreeJson
      })
      .ask[String](treeActor)
      .mapConcat(m => TextMessage(m) :: Nil)
}

