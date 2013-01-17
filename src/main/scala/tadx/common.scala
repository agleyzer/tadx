package tadx

// Shared classes used in multiple components

case object AdIndex {
  val empty = AdIndex(Map.empty)
}

case class AdIndex(pos2ad: Map[String, Seq[Ad]])

case class Ad(name: String, position: String, creative: String)

sealed trait Event
case class NewIndexEvent(index: AdIndex) extends Event
case class AdServedEvent(request: AdRequest, response: AdResponse, timestamp: Long) extends Event

case class AdRequest(positions: Seq[String])
case class AdResponse(ads: Map[String, Ad])
