package ws.gmax.model

sealed trait PersonRequest

case class GetPersonRequest(id: Int, name: Option[String]) extends PersonRequest

case object GetPersonsRequest extends PersonRequest

case class CreatePersonRequest(person: Person) extends PersonRequest

case class UpdatePersonRequest(person: Person) extends PersonRequest

case class DeletePersonRequest(id: Int) extends PersonRequest