package ws.gmax.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import ws.gmax.model._
import ws.gmax.repo.PersonRepo

class PersonActor(personRepo: PersonRepo) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher

  override def preStart(): Unit =
    log.info("Person service is up")

  override def postStop(): Unit =
    log.info("Person service is down")

  override def receive: Receive = {
    case GetPersonRequest(id, name) =>
      name foreach (n => log.info(s"received optional parameter name=$n"))
      personRepo.getPerson(id) pipeTo sender
    case GetPersonsRequest => personRepo.getPersons pipeTo sender
    case DeletePersonRequest(id) => personRepo.deletePerson(id) pipeTo sender
    case CreatePersonRequest(person) => personRepo.insertPerson(person) pipeTo sender
    case UpdatePersonRequest(person) => personRepo.updatePerson(person) pipeTo sender
  }
}

object PersonActor {
  def apply(personRepo: PersonRepo): Props = Props(new PersonActor(personRepo))
}