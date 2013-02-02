package tadx

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.can.server.SprayCanHttpServerApp
import spray.http.{ HttpBody, HttpRequest, HttpResponse }
import spray.http.ContentType._
import spray.http.StatusCodes._
import spray.json._


// Starts business actors, handles http requests.
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

// Driver for the app.
object Tadx extends App with SprayCanHttpServerApp {
  val root = system.actorOf(Props[Manager], name = "root")
  val handler = system.actorOf(Props[RequestHandler])
  newHttpServer(handler) ! Bind(interface = "localhost", port = 8080)
}
