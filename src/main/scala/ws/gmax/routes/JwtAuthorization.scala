package ws.gmax.routes

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.apache.http.HttpHeaders
import ws.gmax.jwt.AuthInfo
import ws.gmax.jwt.JwtToken._
import ws.gmax.service.PersonService

import scala.util.{Failure, Success}

class TokenRoles(enabled: Boolean, roles: Roles) {
  def hasAdminRole = if (enabled) roles.contains(ROLE_ADMIN) else true
  def hasReadRole = if (enabled) roles.contains(ROLE_READ) else true
  def hasWriteRole = if (enabled) roles.contains(ROLE_WRITE) else true
}

object TokenRoles {
  def apply(enabled: Boolean, roles: Roles): TokenRoles = new TokenRoles(enabled, roles)
}

trait JwtAuthorization {
  self: PersonService =>

  private def validateRoles(roles: Roles) = roles.forall(allRoles.contains)

  def withJwtAuthorization(innerRoute: TokenRoles => Route): Route = {

    def authorize(auth: AuthInfo) = {
      auth.authorization match {
        case Some(list) =>
          if (validateRoles(list))
            innerRoute(TokenRoles(enabled = true, list))
          else
            complete(HttpResponse(status = Unauthorized, entity = "Invalid roles"))
        case None => reject
      }
    }

    def onSuccess(info: Either[Throwable, AuthInfo]) = {
      info match {
        case Right(auth) => authorize(auth)
        case Left(th) => failWith(th)
      }
    }

    def checkToken(token: String) = {
      onComplete(validateToken(token).mapTo[Either[Throwable, AuthInfo]]) {
        case Success(info) => onSuccess(info)
        case Failure(th) => failWith(th)
      }
    }

    extractHost { hn =>
      if (hn.toLowerCase == "localhost" && !localhostAuthTest) {
        innerRoute(TokenRoles(enabled = false, Set()))
      } else {
        headerValueByName(HttpHeaders.AUTHORIZATION) { hdr =>
          if (hdr.toLowerCase.startsWith("bearer ") && hdr.length > 7) {
            val token = hdr.substring(7).trim
            checkToken(token)
          } else {
            complete(HttpResponse(status = Unauthorized, entity = "Bearer token is required"))
          }
        }
      }
    }
  }
}

