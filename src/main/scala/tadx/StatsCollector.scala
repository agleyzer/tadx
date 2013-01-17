package tadx

import akka.actor._

object StatsCollector {
  case object GetStats
}

// Keeps counts for every ad served. Listening on AdServedEvent, this
// actor maintains a map of counters. The stats can be retrieved by
// sending GetStats.
class StatsCollector extends Actor with ActorLogging {
  import StatsCollector._

  // ad.name -> count
  private var stats = Map[String, Int]() withDefaultValue(0)

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
