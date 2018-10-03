package ws.gmax.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ws.gmax.model._
import ws.gmax.repo.PersonRepo

class PersonActorSuite extends TestKit(ActorSystem("person-services"))
  with FlatSpecLike
  with ImplicitSender
  with MockitoSugar
  with Matchers
  with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)

  private val probeActor = TestProbe()
  private val deathWatcher = TestProbe()
  private val mockedRepo = mock[PersonRepo]

  val person = Person(1, "John", "New York", 23)

  private val personActor: ActorRef = system.actorOf(Props(
    new PersonActor(mockedRepo) {
      override val receive = {
        case msg: PersonRequest => probeActor.ref forward msg
      }
    }
  ), "personActor")

  deathWatcher.watch(personActor)

  behavior of "PersonsActor"

  it should "forward GetPersonRequest message to probeActor" in {
    personActor ! GetPersonRequest(1, None)
    probeActor.expectMsg(GetPersonRequest(1, None))
  }

  it should "forward GetPersonRequest message to probeActor with text" in {
    personActor ! GetPersonRequest(1, Some("text"))
    probeActor.expectMsg(GetPersonRequest(1, Some("text")))
  }
  it should "forward GetPersonsRequest message to probeActor" in {
    personActor ! GetPersonsRequest
    probeActor.expectMsg(GetPersonsRequest)
  }

  it should "forward CreatePersonRequest message to probeActor" in {
    personActor ! CreatePersonRequest(person)
    probeActor.expectMsg(CreatePersonRequest(person))
  }

  it should "forward UpdatePersonRequest message to probeActor" in {
    personActor ! UpdatePersonRequest(person)
    probeActor.expectMsg(UpdatePersonRequest(person))
  }

  it should "forward DeletePersonRequest message to probeActor" in {
    personActor ! DeletePersonRequest(1)
    probeActor.expectMsg(DeletePersonRequest(1))
  }

  it should "terminate the personActor and the system" in {
    personActor ! PoisonPill
    deathWatcher.expectTerminated(personActor)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}

