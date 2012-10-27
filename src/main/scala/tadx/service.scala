package tadx

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import cc.spray.{Directives, HttpService, SprayCanRootService}
import cc.spray.can.server.HttpServer
import cc.spray.directives.SprayRoute1
import cc.spray.http.HttpHeaders._
import cc.spray.http.MediaTypes._
import cc.spray.http.StatusCodes._
import cc.spray.io.IoWorker
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray.json._

trait Core {
  implicit val timeout = Timeout(5 seconds)

  def actorSystem: ActorSystem

  val root = actorSystem.actorOf(Props[Root], name = "root")

  lazy val adFinder = actorSystem.actorFor("/user/root/adFinder")

  lazy val statsCollector = actorSystem.actorFor("/user/root/statsCollector")
}

trait Api extends Directives { this: Core =>
  import JsonProtocol._

  // format: OFF -- stop autoformatting here - Scalariform chokes on Spray DSL :(
  private val route = {
    pathPrefix("tadx") {
      path("") {
        get { _.complete("TADX!") }
      } ~
      path("ads") {
        parameter('positions) { positions =>
          val req = AdRequest(positions.split(","))
          respondWithMediaType(`application/json`) {
            completeWith {
              (adFinder ? req).mapTo[AdResponse].map { resp =>
                // position->ad becomes position->creative
                val p2c = resp.ads.map { case (pos, ad) => pos -> ad.creative }
                p2c.toJson.compactPrint
              }
            }
          }
        }
      } ~
      path("stats") {
        respondWithMediaType(`application/json`) {
          completeWith {
            (statsCollector ? GetStats).mapTo[Map[String, Int]].map { stats =>
              stats.toJson.compactPrint
            }
          }
        }
      }
    }
  }
  // format: ON

  private val httpService = actorSystem.actorOf(
    props = Props(new HttpService(route)),
    name = "http-service")

  val rootService = actorSystem.actorOf(
    props = Props(new SprayCanRootService(httpService)),
    name = "root-service")
}

trait Web { this: Api with Core =>
  val serverPort = 8080

  val ioWorker = new IoWorker(actorSystem).start()

  val sprayCanServer = actorSystem.actorOf(
    Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(rootService))),
    name = "http-server")

  sprayCanServer ! HttpServer.Bind("0.0.0.0", serverPort)

  actorSystem.registerOnTermination {
    ioWorker.stop()
  }
}

class Application(val actorSystem: ActorSystem) extends Core with Api with Web

object Tadx extends App {
  val actorSystem = ActorSystem("tadx")
  new Application(actorSystem)
}
