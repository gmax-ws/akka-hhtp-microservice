package ws.gmax.swagger

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import io.swagger.models.auth.OAuth2Definition
import io.swagger.models.{Scheme, Swagger}
import ws.gmax.swagger.SwaggerDoc.{scope, scopeDescription}

import scala.concurrent.ExecutionContext.Implicits.global

class SwaggerDoc(services: Seq[Class[_]])(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends SwaggerHttpService {

  private val config = system.settings.config

  private val port  = config.getInt("app.http-port")

  override val apiClasses: Set[Class[_]] = services.toSet

  override val info: Info = Info(version = "1.0")

  override val apiDocsPath: String = system.name

  override val host: String = s"localhost:$port"

  override val schemes: List[Scheme] = List(Scheme.HTTP, Scheme.HTTPS)

  override def swaggerConfig: Swagger = {

    def securityDefinition = {
      val (authorizationUrl, tokenUrl) = SwaggerDoc.authorizationUrlAndTokenUrl(system)
      val auth2Definition = new OAuth2Definition().`implicit`(authorizationUrl)
      auth2Definition.addScope(scope, scopeDescription)
      auth2Definition.setTokenUrl(tokenUrl)
      auth2Definition
    }

    val config = super.swaggerConfig
    config.addSecurityDefinition(SwaggerDoc.securityDefinitionKey, securityDefinition)
    config
  }
}

object SwaggerDoc extends Directives {

  final val securityDefinitionKey = "implicitFlow"
  final val scope = "person"
  final val scopeDescription = "Persons API"

  def authorizationUrlAndTokenUrl(system: ActorSystem): (String, String) = {
    val oauth2server: String = "http://localhost:8088/oauth"
    val authorizationUrl = oauth2server + "/authorize"
    val tokenUrl = oauth2server + "/access_token"
    (authorizationUrl, tokenUrl)
  }

  def apply(services: Seq[Class[_]])(implicit system: ActorSystem, matSystem: ActorMaterializer): Route = {
    val assetsSwagger = pathPrefix(system.name) {
      path("swagger") {
        getFromResourceDirectory("swagger") ~ get {
          redirect("swagger/index.html", StatusCodes.PermanentRedirect)
        }
      }
    } ~ pathPrefix(system.name / "swagger") {
      getFromResourceDirectory("swagger") ~ pathSingleSlash {
        get {
          redirect("index.html", StatusCodes.PermanentRedirect)
        }
      }
    }

    new SwaggerDoc(services).routes ~ cacheControlHeaders(assetsSwagger)
  }

  def cacheControlHeaders(assetsSwagger: Route): Route = { requestContext =>
    assetsSwagger(requestContext).map {
      case Complete(response) => Complete(response.withDefaultHeaders(`Cache-Control`(`no-cache`)))
      case rejected@Rejected(_) => rejected
    }
  }
}
