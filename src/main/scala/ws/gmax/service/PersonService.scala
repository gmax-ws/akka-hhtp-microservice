package ws.gmax.service

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import ws.gmax.Global._
import ws.gmax.model._
import ws.gmax.routes.{OAuth2Routes, PersonServiceRoutes}

import scala.concurrent.Future

class PersonService(personsSupervisorActor: ActorRef, localhostAuth: Boolean) extends PersonServiceRoutes with OAuth2Routes {

  val localhostAuthTest = localhostAuth

  val handler: Route = pathPrefix(system.name) {
    apiRoutes
  } ~ oauthRoutes

  private def sendRequest(request: PersonRequest): Future[Any] = personsSupervisorActor ? request

  def getPersonById(id: Int) =
    sendRequest(GetPersonRequest(id, None))

  def getPersonById(id: Int, name: Option[String]) =
    sendRequest(GetPersonRequest(id, name))

  def getAllPersons() =
    sendRequest(GetPersonsRequest)

  def deletePerson(id: Int) =
    sendRequest(DeletePersonRequest(id))

  def insertPerson(person: Person) =
    sendRequest(CreatePersonRequest(person))

  def updatePerson(person: Person) =
    sendRequest(UpdatePersonRequest(person))

  def getToken(client: String, expireIn: Long = 3600) =
    personsSupervisorActor ? IssueJwtMessage(client, expireIn)

  def validateToken(token: String) =
    personsSupervisorActor ? VerifyJwtMessage(token)
}

object PersonService {
  def apply(personsSupervisorActor: ActorRef, localhostAuth: Boolean): PersonService =
    new PersonService(personsSupervisorActor, localhostAuth)
}
