package tadx

import akka.actor._
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.specs2.mutable.Specification

class AdFinderSpec extends TestKit(ActorSystem()) with ImplicitSender with Specification {
  val ad1 = Ad("ad1", "position1", "creative1")
  val ad2 = Ad("ad2", "position2", "creative2")
  val ad3 = Ad("ad3", "position3", "creative3")

  val adIndex = AdIndex(Map(
    "foo" -> Seq(ad1),
    "bar" -> Seq(ad2),
    "baz" -> Seq(ad3)))

  val indexManager = TestActorRef(new Actor {
    def receive = {
      case Manager.IndexRequest => adIndex
    }
  })

  val adFinder = TestActorRef(new AdFinder(indexManager))

  val underlyingActor = adFinder.underlyingActor

  // val actorRef = system.actorOf(Props[AdFinder])

  "An AdFinder" should {

    "pick a random item" in {
      underlyingActor.pickOne(Seq[Ad](ad1, ad2, ad3)) must beOneOf(ad1, ad2, ad3)
    }

    "fill ad positions" in {
      underlyingActor.index = adIndex // assuming index is already set
      underlyingActor.fillPositions(Seq("foo", "bang")) must_== Map("foo" -> ad1)
    }

    // "respond to a message" in {
    //   adFinder ! AdRequest(Seq("foo", "bar"))
    //   expectMsg(5)
    //   done
    // }
  }
}
