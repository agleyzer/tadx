package tadx

import akka.actor._

// Given AdRequest resturns AdResponse. When initialized, will request
// index update, upon receiving the index it becomes a "realFinder"
// serving only AdRequest's. Note this actor will be killed and
// restarted from scratch when index changes. This design allows this
// actor to keep internal caches etc, and not worry about flushing
// them.
class AdFinder(val manager: ActorRef) extends Actor
  with ActorLogging with AdFinderFunctions {

  import Manager._

  var index: AdIndex = AdIndex.empty

  override def preStart() {
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
