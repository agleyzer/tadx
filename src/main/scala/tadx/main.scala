package tadx

import akka.actor._
import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.routing.RoundRobinRouter
import akka.util.duration._
import cc.spray.json._
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import scala.io.Source
import scala.util.Random

case class AdRequest(positions: Seq[String])

case class AdResponse(ads: Map[String, Ad])

case class Ad(name: String, position: String, creative: String)

object JsonProtocol extends DefaultJsonProtocol {
  implicit val adFormat = jsonFormat3(Ad)
}

case class AdIndex(pos2ad: Map[String, Seq[Ad]])

case class AdServedEvent(request: AdRequest, response: AdResponse, timestamp: Long)

case class NewIndexEvent(index: AdIndex)

case object IndexRequest

case object CheckIndexFile

case object GetStats

// Monitors file system for changes in the index file. When a change
// is detected, loads ads from the file, creates a map of
// position->Seq[Ad] and sends a notification event to eventbus.
class IndexLoader(indexFile: File) extends Actor with ActorLogging {
  var lastUpdate: Long = 0 // last load timestamp

  context.system.scheduler.schedule(0 seconds, 1 second, self, CheckIndexFile)

  // loads indexFile as JSON
  def loadIndexFile: Seq[Ad] = {
    import JsonProtocol._
    val json = Source.fromFile(indexFile).mkString.asJson
    json.convertTo[Seq[Ad]]
  }

  def receive = {
    case CheckIndexFile => {
      val lastModified = indexFile.lastModified
      if (lastUpdate < lastModified) {
        log.info("loading new index")
        val ads = loadIndexFile
        val index = AdIndex(ads.groupBy(_.position))
        context.system.eventStream.publish(NewIndexEvent(index))
        lastUpdate = lastModified
      }
    }
  }
}

// Keeps counts for every ad served. Listening on AdServedEvent, this
// actor maintains a map of counters. The stats can be retrieved by
// sending GetStats.
class StatsCollector extends Actor with ActorLogging {
  // ad.name -> count
  var stats = Map[String, Int]() withDefaultValue (0)

  context.system.eventStream.subscribe(self, classOf[AdServedEvent])

  def receive = {
    case AdServedEvent(request, response, _) => {
      response.ads.values.foreach { ad =>
        stats += ad.name -> (stats(ad.name) + 1)
      }
    }
    case GetStats => sender ! stats
  }
}

// Logs every ad served in a text file. All ads served in one response
// are logged as one line.
class AdLogger(val logFile: File) extends Actor with ActorLogging {
  var writer: PrintWriter = _

  context.system.eventStream.subscribe(self, classOf[AdServedEvent])

  override def preStart() = {
    writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)), true)
  }

  override def postStop() = {
    writer.close()
  }

  // FORMAT: timestamp: pos|ad, pos|ad, pos|ad
  def logString(e: AdServedEvent) = {
    e.timestamp + ": " + e.request.positions.map { position =>
      val adOption = e.response.ads.get(position)
      val adName = adOption.map(_.name).getOrElse("FAILED")
      position + "|" + adName
    }.mkString(", ")
  }

  def receive = {
    case e: AdServedEvent => writer.println(logString(e))
  }
}

// Given AdRequest resturns AdResponse. When initialized, will request
// index update, upon receiving the index it becomes a "realFinder"
// serving only AdRequest's. Note this actor will be killed and
// restarted from scratch when index changes. This design allows this
// actor to keep internal caches etc, and not worry about flushing
// them.
class AdFinder(val indexManager: ActorRef) extends Actor with ActorLogging {
  var index: AdIndex = AdIndex(Map.empty)

  // picks a random Ad from a collection
  def randomAd(coll: Seq[Ad]): Ad = coll(scala.util.Random.nextInt(coll.size))

  // given a set of positions returns a Map position -> creative
  def fillPositions(positions: Seq[String]): Map[String, Ad] = {
    positions.map { pos =>
      index.pos2ad.get(pos).map { ads =>
        val ad = randomAd(ads)
        pos -> ad
      }
    }.flatten.toMap
  }

  override def preStart() = {
    log.info("asking manager for index")
    indexManager ! IndexRequest
  }

  // this will become our "working" receive
  def realFinder: Receive = {
    case request: AdRequest => {
      val response = AdResponse(fillPositions(request.positions))
      sender ! response
      context.system.eventStream.publish(AdServedEvent(request, response, System.currentTimeMillis))
    }
  }

  // initial state, asking for index from manager
  def receive = {
    case index: AdIndex => {
      log.info("got index from manager " + index)
      this.index = index

      log.info("becoming a real finder")
      context.become(realFinder)
    }
  }
}

// Creates all other actors, also manages index updates.
// TODO: consider splitting into index manager and supervisor
class Root extends Actor with ActorLogging {
  var index: AdIndex = AdIndex(Map.empty)

  context.system.eventStream.subscribe(self, classOf[NewIndexEvent])

  val indexLoader = context.actorOf(Props { new IndexLoader(new File("ads.json")) },
    name = "indexLoader")

  val adFinder = context.actorOf(Props { new AdFinder(self) }.
    withRouter(RoundRobinRouter(nrOfInstances = 2)), // <= # of cores
    name = "adFinder")

  val statsCollector = context.actorOf(Props[StatsCollector], name = "statsCollector")

  val adLogger = context.actorOf(Props { new AdLogger(new File("ads.log")) }, name = "adLogger")

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
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
