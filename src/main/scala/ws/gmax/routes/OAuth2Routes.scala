package ws.gmax.routes

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import ws.gmax.jwt.AuthInfo
import ws.gmax.service.PersonService

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

case class AccessToken(access_token: String, expires_in: Long, token_type: String, scope: String)

case class OAuth2Error(error: String)

trait OAuth2Protocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val accessToken: RootJsonFormat[AccessToken] = jsonFormat4(AccessToken)
  implicit val errorFormat: RootJsonFormat[OAuth2Error] = jsonFormat1(OAuth2Error)
}

trait OAuth2Routes extends OAuth2Protocol {
  self: PersonService =>

  val applicationId = "implicitFlow"
  val expireIn: Long = FiniteDuration(1, TimeUnit.HOURS).toSeconds

  val tokenType = "bearer"
  val scope = "person"
  val realm = "gmax"
  val clients = Set("admin", "user", "guest")

  val oauthRoutes: Route = pathPrefix("oauth") {
    path("access_token") {
      post {
        formFields('client_id, 'code) { (clientId, code) =>
          if (applicationId == code) {
            onComplete(getToken(clientId).mapTo[String]) {
              case Success(token) => complete(AccessToken(token, expireIn, tokenType, scope))
              case Failure(th) => complete(OAuth2Error(th.getMessage))
            }
          } else {
            complete((BadRequest, s"Invalid application id: $code"))
          }
        }
      }
    } ~ path("validation") {
      get {
        parameters('token) { token =>
          onComplete(validateToken(token).mapTo[Either[Throwable, AuthInfo]]) {
            case Success(info) => complete(info)
            case Failure(th) => failWith(th)
          }
        }
      }
    } ~ path("authorize") {
      get {
        parameters('client_id, 'redirect_uri, 'response_type, 'realm_id, 'state, 'scope) {
          (clientId, redirectUri, responseType, realmId, state, scope) =>
            if (isValidRequest(realmId, scope, clientId)) {
              val uri = s"$redirectUri?code=$applicationId&state=$state"
              redirect(uri, PermanentRedirect)
            } else {
              complete((NotFound, OAuth2Error(s"Not authorized client: $clientId scope: $scope realm: $realmId")))
            }
        }
      }
    }
  }

  private def isValidRequest(realm_id: String, scope: String, client_id: String) = {
    realm_id == realm && scope == scope && clients.contains(client_id)
  }
}
