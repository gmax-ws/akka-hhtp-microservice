
package ws.gmax.service

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import ws.gmax.model._
import ws.gmax.routes.PersonRejection
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.pipe
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.Future

class PersonServiceSuite extends Matchers
  with MockitoSugar
  with FlatSpecLike
  with BeforeAndAfterAll
  with ScalatestRouteTest {

  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)
  implicit val host = DefaultHostInfo(Host("localhost"), false)

  val person1 = Person(1, "John", "New York", 23)
  val person2 = Person(2, "Greg", "Atlanta", 53)

  val jsonRequest1 = """{"id":1,"name":"John","address":"New York","age":23}"""
  val jsonRequest2 = """{"id":2,"name":"Greg","address":"Atlanta","age":53}"""

  private val testActor = TestActorRef(new Actor {
    override def receive: PartialFunction[Any, Unit] = {
      case GetPersonRequest(5, None) => Future.successful(None) pipeTo sender
      case GetPersonRequest(1, None) => Future.successful(Some(person1)) pipeTo sender
      case GetPersonsRequest => Future.successful(Persons(List(person1, person2))) pipeTo sender
      case DeletePersonRequest(1) => Future.successful(true) pipeTo sender
      case CreatePersonRequest(person) => Future.successful(person == person1) pipeTo sender
      case UpdatePersonRequest(person) => Future.successful(person == person2) pipeTo sender
    }
  })

  private val personsRoute = PersonService(testActor, false).apiRoutes

  override def createActorSystem(): ActorSystem = {
    ActorSystem("person-services")
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  it should "reject GET call for id=5" in {
    Get("/person/5") ~>
      personsRoute ~> check {
      rejection shouldBe PersonRejection("Person not found id: 5")
    }
  }

  it should "return OK on GET call for id=1" in {
    Get("/person/1") ~>
      personsRoute ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should "return OK on GET call getting list of persons" in {
    Get("/persons") ~>
      personsRoute ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should "return OK on POST inserting person1" in {
    val postRequest = HttpRequest(HttpMethods.POST, uri = "/person",
      entity = HttpEntity(MediaTypes.`application/json`, jsonRequest1))
    postRequest ~>
      personsRoute ~> check {
      status.isSuccess() shouldBe true
    }
  }

  it should "reject POST inserting person2" in {
    val postRequest = HttpRequest(HttpMethods.POST, uri = "/person",
      entity = HttpEntity(MediaTypes.`application/json`, jsonRequest2))
    postRequest ~>
      personsRoute ~> check {
      rejection shouldBe PersonRejection("Insert Person(2,Greg,Atlanta,53) failed")
    }
  }

  it should "reject PUT updating person1" in {
    val postRequest = HttpRequest(HttpMethods.PUT, uri = "/person", entity =
      HttpEntity(MediaTypes.`application/json`, jsonRequest1))
    postRequest ~>
      personsRoute ~> check {
      rejection shouldBe PersonRejection("Update Person(1,John,New York,23) failed")
    }
  }

  it should "return OK on PUT updating person1" in {
    val postRequest = HttpRequest(HttpMethods.PUT, uri = "/person",
      entity = HttpEntity(MediaTypes.`application/json`, jsonRequest2))
    postRequest ~>
      personsRoute ~> check {
      status.isSuccess() shouldBe true
    }
  }

  it should "return OK on GET deleting person having id=1" in {
    Delete("/person/1") ~>
      personsRoute ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
