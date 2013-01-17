package tadx

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import language.postfixOps
import scala.concurrent.duration._
import spray.http.{HttpRequest, HttpResponse, HttpBody}
import spray.http.HttpMethods._
import spray.http.ContentType._
import spray.http.StatusCodes._
import spray.can.server.SprayCanHttpServerApp
import spray.json._

class RequestHandler extends Actor {
  import Json._
  import context.dispatcher

  private implicit val timeout = Timeout(5 seconds)

  private val separator = ",".r                 // regex

  private lazy val adFinder = context.system.actorFor("/user/root/adFinder")
  private lazy val statsCollector = context.system.actorFor("/user/root/statsCollector")

  private def jsonResponse(json: JsValue) = HttpResponse (entity = HttpBody(`application/json`, json.compactPrint))

  private def serveAds(request: HttpRequest, sender: ActorRef) = {
    val positions = request.queryParams.getOrElse("positions", "")
    val adreq = AdRequest(separator.split(positions))

    (adFinder ? adreq).mapTo[AdResponse].map { resp =>
      // position->ad becomes position->creative
      val p2c = resp.ads.map { case (pos, ad) => pos -> ad.creative }
      sender ! jsonResponse(p2c.toJson)
    }
  }

  private def serveStats(sender: ActorRef) = {
    (statsCollector ? StatsCollector.GetStats).mapTo[Map[String, Int]].map { stats =>
      sender ! jsonResponse(stats.toJson)
    }
  }

  def receive = {
    case r: HttpRequest =>
      val pr = r.parseAll

      pr.path match {
        case "/tadx/ads" => serveAds(pr, sender)
        case "/tadx/stats" => serveStats(sender)
        case _ => sender ! HttpResponse(status = NotFound, entity = pr.path)
      }
  }
}


object Tadx extends App with SprayCanHttpServerApp {
  val root = system.actorOf(Props[Root], name = "root")
  val handler = system.actorOf(Props[RequestHandler])
  newHttpServer(handler) ! Bind(interface = "localhost", port = 8080)
}
