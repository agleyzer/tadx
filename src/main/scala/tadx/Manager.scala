package tadx

import akka.actor._
import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.routing.RoundRobinRouter
import language.postfixOps
import scala.concurrent.duration._
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import scala.util.Random

object Manager {
  case object IndexRequest
}

// Creates all other actors, also manages index updates.
// TODO: consider splitting into index manager and supervisor
class Manager extends Actor with ActorLogging {
  import Manager._

  private var index: AdIndex = AdIndex.empty // initial index

  context.system.eventStream.subscribe(self, classOf[NewIndexEvent])

  val indexLoader = context.actorOf(
    Props { new IndexLoader(new File("ads.json")) },
    name = "indexLoader")

  val adFinder = context.actorOf(
    Props { new AdFinder(self) }.
    withRouter(RoundRobinRouter(nrOfInstances = 2)), // <= # of cores
    name = "adFinder")

  val statsCollector = context.actorOf(
    Props[StatsCollector],
    name = "statsCollector")

  val adLogger = context.actorOf(
    Props { new AdLogger(new File("ads.log")) },
    name = "adLogger")


  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
      case _: ActorKilledException => Restart
      case _: Exception => Restart
      case _ => Escalate
    }


  def receive = {
    case NewIndexEvent(newIndex) => {
      log.info("received new index " + newIndex)
      this.index = newIndex
      adFinder ! Kill // restart finder(s)
    }

    case IndexRequest => {
      log.info("got index request")
      sender ! index
    }
  }
}
