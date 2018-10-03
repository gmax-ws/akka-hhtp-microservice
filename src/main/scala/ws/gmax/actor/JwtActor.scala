package ws.gmax.actor

import akka.actor.{Actor, ActorLogging}
import ws.gmax.jwt.JwtToken
import ws.gmax.model._

class JwtActor extends Actor with ActorLogging {

  val jwt = JwtToken()

  override def receive: Receive = {
    case IssueJwtMessage(client, expireIn) => sender ! jwt.issue(client, expireIn)
    case VerifyJwtMessage(token) => sender ! jwt.validate(token)
  }
}
