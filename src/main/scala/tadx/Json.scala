package tadx

import spray.json._

object Json extends DefaultJsonProtocol {
  implicit val adFormat = jsonFormat3(Ad)

  def parseAds(s: String): Seq[Ad] = s.asJson.convertTo[Seq[Ad]]
}
