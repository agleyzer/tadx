package tadx

import akka.actor._
import scala.util.Random


// Given AdRequest resturns AdResponse. When initialized, will request
// index update, upon receiving the index it becomes a "realFinder"
// serving only AdRequest's. Note this actor will be killed and
// restarted from scratch when index changes. This design allows this
// actor to keep internal caches etc, and not worry about flushing
// them.
class AdFinder(val indexManager: ActorRef) extends Actor with ActorLogging {
  import Root._

  private var index: AdIndex = AdIndex.empty

  // picks a random Ad from a collection
  private def pickOne(coll: Seq[Ad]): Ad = coll(scala.util.Random.nextInt(coll.size))

  // given a set of positions returns a Map position -> creative
  private def fillPositions(positions: Seq[String]): Map[String, Ad] = {
    positions.map { pos =>
      index.pos2ad.get(pos).map { ads =>
        val ad = pickOne(ads)
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
      context.system.eventStream.publish(AdServedEvent(request, response))
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
