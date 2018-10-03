package ws.gmax.repo

import com.datastax.driver.core.Session
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.scalatest.{AsyncFlatSpecLike, BeforeAndAfterAll, Matchers}
import ws.gmax.cassandra.EmbeddedCassandra
import ws.gmax.model.Person

import scala.util.{Failure, Success}

/**
  * Test PersonRepo with embedded Cassandra support
  */
class PersonRepoSuite extends EmbeddedCassandra with AsyncFlatSpecLike with BeforeAndAfterAll with Matchers {

  private var personRepo: PersonRepo = _
  private var cassandraSession: Session = _

  override def beforeAll(): Unit = {
    val dataSet = new ClassPathCQLDataSet("movie-test.cql", true, true, "movie")
    cassandraSession = startUp(port = 9142, dataSet = Some(dataSet))
    personRepo = PersonRepo(cassandraSession, isAsync = false)
  }

  behavior of "PersonRepo"

  it should "return person having id=1" in {
    personRepo.getPerson(1) transformWith {
      case Success(foundPerson) =>
        foundPerson match {
          case Some(person) => person shouldBe Person(1, "Jurgen Bosch", "Sibiu", 25)
          case None => fail
        }
      case Failure(th) => fail(th)
    }
  }

  it should "return a list of all persons" in {
    personRepo.getPersons transformWith {
      case Success(persons) => persons.persons.size shouldBe 2
      case Failure(th) => fail(th)
    }
  }

  it should "insert a new person" in {
    val person = Person(5, "Vlad Gheorghe", "Cluj", 35)
    personRepo.insertPerson(person) transformWith {
      case Success(wasInserted) => wasInserted shouldBe true
      case Failure(th) => fail(th)
    }
  }

  it should "return a list of all persons including the already inserted person" in {
    personRepo.getPersons transformWith {
      case Success(persons) => persons.persons.size shouldBe 3
      case Failure(th) => fail(th)
    }
  }

  it should "fail to insert a new person" in {
    val person = Person(5, "Vlad Gheorghe", "Cluj", 35)
    personRepo.insertPerson(person) transformWith {
      case Success(wasInserted) => wasInserted shouldBe false
      case Failure(th) => fail(th)
    }
  }

  it should "update inserted person" in {
    val person = Person(5, "Vlad Gheorghe", "Bucuresti", 45)
    personRepo.updatePerson(person) transformWith {
      case Success(wasUpdated) => wasUpdated shouldBe true
      case Failure(th) => fail(th)
    }
  }

  it should "not update person having id=7" in {
    val person = Person(7, "Ionel Dobra", "Craiova", 52)
    personRepo.updatePerson(person) transformWith {
      case Success(wasUpdated) => wasUpdated shouldBe false
      case Failure(th) => fail(th)
    }
  }

  it should "return updated person having id=5" in {
    personRepo.getPerson(5) transformWith {
      case Success(foundPerson) =>
        foundPerson match {
          case Some(person) => person shouldBe Person(5, "Vlad Gheorghe", "Bucuresti", 45)
          case None => fail
        }
      case Failure(th) => fail(th)
    }
  }

  it should "delete person having id=1" in {
    personRepo.deletePerson(1) transformWith {
      case Success(wasDeleted) => wasDeleted shouldBe true
      case Failure(th) => fail(th)
    }
  }

  it should "not return deleted person having id=1" in {
    personRepo.getPerson(1) transformWith {
      case Success(foundPerson) =>
        foundPerson match {
          case Some(_) => fail
          case None => succeed

        }
      case Failure(th) => fail(th)
    }
  }

  override def afterAll(): Unit = {
    shutDown(cassandraSession)
  }
}
