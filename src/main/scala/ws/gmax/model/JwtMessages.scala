package ws.gmax.model

sealed trait JwtMessage

case class IssueJwtMessage(client: String, expireIn: Long) extends JwtMessage

case class VerifyJwtMessage(token: String) extends JwtMessage
