package ws.gmax.actor

import akka.actor.{ActorRef, DeadLetter, Props, Terminated}
import akka.routing.FromConfig
import com.datastax.driver.core.Session
import ws.gmax.model._
import ws.gmax.repo.PersonRepo

class PersonSupervisorActor(session: Session) extends AbstractSupervisorActor {

  import context.dispatcher

  val personRepo = PersonRepo(session)

  /** Create actors */
  val personsActor: ActorRef = context.actorOf(FromConfig.props(PersonActor(personRepo)), "personActor")

  val jwtActor: ActorRef = context.actorOf(Props[JwtActor], "jwtActor")

  override def preStart(): Unit = {
    super.preStart()
    log.info("Person supervisor is up")
    context.watch(personsActor)
  }

  override def postStop(): Unit = {
    log.info("Person supervisor is down")
    super.postStop()
  }

  override def receive: Receive = {
    case message: GetPersonRequest => personsActor forward message

    case message: GetPersonsRequest.type => personsActor forward message

    case message: DeletePersonRequest => personsActor forward message

    case message: CreatePersonRequest => personsActor forward message

    case message: UpdatePersonRequest => personsActor forward message

    case message: IssueJwtMessage => jwtActor forward  message

    case message: VerifyJwtMessage => jwtActor forward  message

    case DeadLetter(message, sender, recipient) =>
      log.warning(s"The $recipient is not able to process message $message received from $sender")

    case Terminated(actor) =>
      log.error(s"Stopping actor and shutting down system because of actor: ${actor.path}")
      context.stop(self)
      context.system.terminate

    case _ =>
      log.error("Message is not processed")
  }
}

object PersonSupervisorActor {
  def apply(session: Session): Props = Props(new PersonSupervisorActor(session))
}
