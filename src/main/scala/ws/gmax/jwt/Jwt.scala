package ws.gmax.jwt

import com.typesafe.scalalogging.LazyLogging
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json._

class JwtToken extends DefaultJsonProtocol with LazyLogging {

  implicit val authInfoFormat = jsonFormat3(AuthInfo)

  def clientRoles(client: String) =
    client match {
      case "admin" => allRoles
      case "user" => Set(ROLE_READ, ROLE_WRITE)
      case "guest" => Set(ROLE_READ)
      case _ => Set()
    }

  def issue(client: String, expireIn: Long): String = {

    val claim = JwtClaim()
      .by("http://gmax.go.ro")
      .about(client)
      .to(client)
      .issuedNow
      .expiresIn(expireIn)
      .+("authorization", clientRoles(client))

    Jwt.encode(claim, privateKey, JwtAlgorithm.RS256)
  }

  def verifyToken(token: String, secret: String): Either[Throwable, AuthInfo] =
    Jwt.decode(token, secret, Seq(JwtAlgorithm.RS256)).map { decodedJson =>
      logger.info(s"Decoded json token $decodedJson")
      decodedJson.parseJson.convertTo[AuthInfo]
    }.toEither

  def validate(token: String): Either[Throwable, AuthInfo] = verifyToken(token, publicKey)
}

object JwtToken {
  def apply(): JwtToken = new JwtToken()
}
