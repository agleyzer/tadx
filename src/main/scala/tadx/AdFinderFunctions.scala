package tadx

import scala.util.Random

// Business logic for resolving ads.
trait AdFinderFunctions {
  def index: AdIndex

  // picks a random T from a collection
  def pickOne(coll: Seq[Ad]) = coll(Random.nextInt(coll.size))

  // given a set of positions returns a Map position -> creative
  def fillPositions(positions: Seq[String]): Map[String, Ad] = {
    {
      for {
        p <- positions
        candidates <- index.pos2ad.get(p)
      } yield p -> pickOne(candidates)
    }.toMap
  }
}
