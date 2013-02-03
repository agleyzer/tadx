package tadx

import org.specs2.mutable.Specification

class AdFinderFunctionsSpec extends Specification {
  val ad1 = Ad("ad1", "position1", "creative1")
  val ad2 = Ad("ad2", "position2", "creative2")
  val ad3 = Ad("ad3", "position3", "creative3")

  val adIndex = AdIndex(Map(
    "foo" -> Seq(ad1),
    "bar" -> Seq(ad2),
    "baz" -> Seq(ad3)))

  val adFinder = new AdFinderFunctions {
    val index = adIndex
  }

  "AdFinderFunctions" should {

    "pick one ad from a bunch of candidates" in {
      adFinder.pickOne(Seq[Ad](ad1, ad2, ad3)) must beOneOf(ad1, ad2, ad3)
    }

    "fill all possible ad positions" in {
      adFinder.fillPositions(Seq("foo", "bang")) must_== Map("foo" -> ad1)
    }
  }
}
