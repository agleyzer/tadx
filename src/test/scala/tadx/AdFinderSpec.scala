package tadx

import akka.actor._
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.specs2.mutable.Specification

class AdFinderSpec extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with Specification {

  import Manager._

  val ad1 = Ad("ad1", "position1", "creative1")

  val adIndex = AdIndex(Map("foo" -> Seq(ad1)))

  "An AdFinder" should {
    "respond to an ad request" in {
      val adFinder = system.actorOf(Props { new AdFinder(testActor) })

      expectMsg(IndexRequest)

      lastSender ! adIndex

      adFinder ! AdRequest(Seq("foo", "bar"))

      expectMsg(AdResponse(Map("foo" -> ad1)))

      system.stop(adFinder)

      done
    }
  }
}
