package tadx

import akka.actor._
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.specs2.mutable.Specification

class AdFinderSpec extends TestKit(ActorSystem()) with ImplicitSender with Specification {
  val ad1 = Ad("ad1", "position1", "creative1")

  val adIndex = AdIndex(Map("foo" -> Seq(ad1)))

  val indexManager = TestActorRef(new Actor {
    def receive = {
      case Manager.IndexRequest => sender ! adIndex
    }
  })

  val adFinder = TestActorRef(new AdFinder(indexManager))

  "An AdFinder" should {
    "respond to an ad request" in {
       adFinder ! AdRequest(Seq("foo", "bar"))
       val expectedResponse = AdResponse(Map("foo" -> ad1))
       expectMsg(expectedResponse)
       done
    }
  }
}
