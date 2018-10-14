package ws.gmax.repo

import ws.gmax.model.{Person, Persons}

import scala.concurrent.Future

trait PersonRepo {
  type Id = Int

  def getPerson(id: Id): Future[Option[Person]]

  def getPersons: Future[Persons]

  def insertPerson(person: Person): Future[Boolean]

  def updatePerson(person: Person): Future[Boolean]

  def deletePerson(id: Id): Future[Boolean]
}

