package tadx

import akka.actor._
import java.io.File
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object IndexLoader {
  case object CheckIndexFile
}

// Monitors file system for changes in the index file. When a change
// is detected, loads ads from the file, creates a map of
// position->Seq[Ad] and sends a notification event to eventbus.
class IndexLoader(indexFile: File) extends Actor with ActorLogging {
  import IndexLoader._
  import context.dispatcher

  private var lastUpdate: Long = 0 // last load timestamp

  context.system.scheduler.schedule(0 seconds, 1 second, self, CheckIndexFile)

  // loads indexFile as JSON
  private def loadAdsFromFile(): Seq[Ad] = {
    val s = Source.fromFile(indexFile).mkString
    Json.parseAds(s)
  }

  def receive = {
    case CheckIndexFile => {
      val lastModified = indexFile.lastModified
      if (lastUpdate < lastModified) {
        log.info("loading new index")
        val ads = loadAdsFromFile()
        val index = AdIndex(ads.groupBy(_.position))
        context.system.eventStream.publish(NewIndexEvent(index))
        lastUpdate = lastModified
      }
    }
  }
}
