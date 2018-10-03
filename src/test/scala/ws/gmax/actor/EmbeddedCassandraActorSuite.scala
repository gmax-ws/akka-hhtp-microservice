package ws.gmax.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.datastax.driver.core.Session
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ws.gmax.model.{CassandraDataSet, CassandraSettings, ShutdownCassandra, StartupCassandra}

class EmbeddedCassandraActorSuite extends TestKit(ActorSystem("person-services"))
  with FlatSpecLike
  with ImplicitSender
  with MockitoSugar
  with Matchers
  with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)
  val dataSet = CassandraDataSet("movie-test.cql", "movie")
  val cassandraSettings = CassandraSettings(port = 9142, dataSet = dataSet)
  private val probeActor = TestProbe()
  private val deathWatcher = TestProbe()
  private val mockedSession: Session = mock[Session]

  private val embeddedCassandraActor: ActorRef = system.actorOf(Props(new EmbeddedCassandraActor(cassandraSettings) {
    override def receive: Receive ={
      case msg @ StartupCassandra => probeActor.ref forward msg
      case msg @ ShutdownCassandra(_) => probeActor.ref forward msg
    }
  }),
    "embeddedCassandraActor")

  deathWatcher.watch(embeddedCassandraActor)

  behavior of "EmbeddedCassandraActor"

  it should "forward StartupCassandra message to probeActor" in {
    embeddedCassandraActor ! StartupCassandra
    probeActor.expectMsg(StartupCassandra)
  }

  it should "forward ShutdownCassandra message to probeActor" in {
    embeddedCassandraActor ! ShutdownCassandra(mockedSession)
    probeActor.expectMsg(ShutdownCassandra(mockedSession))
  }

  it should "terminate the personActor and the system" in {
    embeddedCassandraActor ! PoisonPill
    deathWatcher.expectTerminated(embeddedCassandraActor)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}

