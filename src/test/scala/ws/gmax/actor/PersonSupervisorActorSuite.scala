package ws.gmax.actor


import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.datastax.driver.core.Session
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ws.gmax.model._

class PersonSupervisorActorSuite extends TestKit(ActorSystem("person-services"))
  with FlatSpecLike
  with ImplicitSender
  with MockitoSugar
  with Matchers
  with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)

  private val childActor = TestProbe()
  private val deathWatcher = TestProbe()
  private val mockedSession: Session = mock[Session]
  val person = Person(1, "John", "New York", 23)

  private val personSupervisorActor: ActorRef = system.actorOf(Props(
    new PersonSupervisorActor(mockedSession) {
      override val personsActor = childActor.ref
    }
  ), "personSupervisorActor")

  deathWatcher.watch(personSupervisorActor)

  behavior of "PersonSupervisorActor"

  it should "forward GetPersonRequest message to personActor" in {
    personSupervisorActor ! GetPersonRequest(1, None)
    childActor.expectMsg(GetPersonRequest(1, None))
  }

  it should "forward GetPersonRequest message to personActor with text" in {
    personSupervisorActor ! GetPersonRequest(1, Some("text"))
    childActor.expectMsg(GetPersonRequest(1, Some("text")))
  }
  it should "forward GetPersonsRequest message to personActor" in {
    personSupervisorActor ! GetPersonsRequest
    childActor.expectMsg(GetPersonsRequest)
  }

  it should "forward CreatePersonRequest message to personActor" in {
    personSupervisorActor ! CreatePersonRequest(person)
    childActor.expectMsg(CreatePersonRequest(person))
  }

  it should "forward UpdatePersonRequest message to personActor" in {
    personSupervisorActor ! UpdatePersonRequest(person)
    childActor.expectMsg(UpdatePersonRequest(person))
  }

  it should "forward DeletePersonRequest message to personActor" in {
    personSupervisorActor ! DeletePersonRequest(1)
    childActor.expectMsg(DeletePersonRequest(1))
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}

