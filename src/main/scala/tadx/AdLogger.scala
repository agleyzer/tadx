package tadx

import akka.actor._
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

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

  // FORMAT: timestamp: pos|ad, pos|ad, pos|ad...
  def logString(e: AdServedEvent) = {
    // for each position in request, see if it's filled in response
    e.timestamp + ": " + e.request.positions.map { position =>
      val ad = e.response.ads.get(position) // Option[Ad]
      val adName = ad.map(_.name).getOrElse("FAILED")
      position + "|" + adName
    }.mkString(", ")
  }

  def receive = {
    case e: AdServedEvent => writer.println(logString(e))
  }
}
