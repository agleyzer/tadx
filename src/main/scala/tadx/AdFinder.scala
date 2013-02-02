package tadx

import akka.actor._
import scala.util.Random


// Given AdRequest resturns AdResponse. When initialized, will request
// index update, upon receiving the index it becomes a "realFinder"
// serving only AdRequest's. Note this actor will be killed and
// restarted from scratch when index changes. This design allows this
// actor to keep internal caches etc, and not worry about flushing
// them.
class AdFinder(val manager: ActorRef) extends Actor with ActorLogging {
  import Manager._

  private[tadx] var index: AdIndex = AdIndex.empty

  // picks a random T from a collection
  private[tadx] def pickOne(coll: Seq[Ad]) = coll(Random.nextInt(coll.size))

  // given a set of positions returns a Map position -> creative
  private[tadx] def fillPositions(positions: Seq[String]): Map[String, Ad] = {
    positions.map { pos =>
      index.pos2ad.get(pos).map { ads =>
        val ad = pickOne(ads)
        pos -> ad
      }
    }.flatten.toMap
  }

  override def preStart() = {
    log.info("asking manager for index")
    manager ! IndexRequest
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
