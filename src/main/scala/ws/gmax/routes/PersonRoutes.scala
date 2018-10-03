package ws.gmax.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, failWith, get, onComplete, path, post, put, reject, _}
import akka.http.scaladsl.server._
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation
import io.swagger.annotations._
import javax.ws.rs.Path
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import ws.gmax.jwt.AuthInfo
import ws.gmax.model._
import ws.gmax.service.PersonService
import ws.gmax.swagger.SwaggerDoc

import scala.util.{Failure, Success}

final case class PersonRejection(msg: String) extends Rejection
final case class SimpleResponse(message: String, timestamp: String =
  ISODateTimeFormat.dateTime().print(new DateTime()))

trait PersonJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val personFormat: RootJsonFormat[Person] = jsonFormat4(Person)
  implicit val personsFormat: RootJsonFormat[Persons] = jsonFormat1(Persons)
  implicit val authInfoFormat: RootJsonFormat[AuthInfo] = jsonFormat3(AuthInfo)
  implicit val simpleResponseFormat: RootJsonFormat[SimpleResponse] = jsonFormat2(SimpleResponse)
}

@Api(value = "Person API")
@Path("/person-services")
@SwaggerDefinition(
  consumes = Array("application/json"),
  produces = Array("application/json"),
  securityDefinition = new SecurityDefinition(apiKeyAuthDefinitions = Array(
        new ApiKeyAuthDefinition(key = "implicitFlow", name = "Authorization", in = ApiKeyLocation.HEADER)
  )),
  info = new Info(
    title = "Person Services",
    version = "1.0",
    description = "API to provide person operations"))
trait PersonServiceRoutes extends PersonJsonProtocol with JwtAuthorization {

  self: PersonService =>

  @Path("/person/{id}")
  @ApiOperation(
    value = "get a person by id as path param",
    nickname = "person",
    httpMethod = "GET",
    code = 200,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Person])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "integer", paramType = "path")))
  @ApiResponses(Array(
    new ApiResponse(
      code = 200,
      message = "Return person if found",
      response = classOf[ws.gmax.model.Person]),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def personRoute: Route = path("person" / IntNumber) { id =>
    get {
      withJwtAuthorization { roles =>
        if (roles.hasReadRole) {
          onComplete(self.getPersonById(id).mapTo[Option[Person]]) {
            case Success(foundPerson) =>
              foundPerson match {
                case Some(person) => complete(person)
                case None => reject(PersonRejection(s"Person not found id: $id"))
              }
            case Failure(th) => failWith(th)
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/person")
  @ApiOperation(
    value = "get a person by id as query param",
    nickname = "person",
    httpMethod = "GET",
    code = 200,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Person])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "integer", paramType = "query"),
    new ApiImplicitParam(name = "name", required = false, dataType = "string", paramType = "query")))
  @ApiResponses(Array(
    new ApiResponse(
      code = 200,
      message = "Return person if found",
      response = classOf[ws.gmax.model.Person]),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def personQueryRoute: Route = path("person") {
    get {
      withJwtAuthorization { roles =>
        if (roles.hasReadRole) {
          parameters('id.as[Int], 'name.?) { (id, name) =>
            onComplete(self.getPersonById(id, name).mapTo[Option[Person]]) {
              case Success(foundPerson) =>
                foundPerson match {
                  case Some(person) => complete(person)
                  case None => reject(PersonRejection(s"Person not found id: $id"))
                }
              case Failure(th) => failWith(th)
            }
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/persons")
  @ApiOperation(
    value = "get all persons",
    nickname = "all",
    httpMethod = "GET",
    code = 200,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Persons])
  @ApiResponses(Array(
    new ApiResponse(
      code = 200,
      message = "list of persons",
      response = classOf[ws.gmax.model.Persons]),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def personsRoute: Route = path("persons") {
    get {
      withJwtAuthorization { roles =>
        if (roles.hasReadRole) {
          onComplete(self.getAllPersons().mapTo[Persons]) {
            case Success(persons) => complete(persons)
            case Failure(th) => failWith(th)
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/person/{id}")
  @ApiOperation(
    value = "delete an existing person by id",
    nickname = "deletion",
    httpMethod = "DELETE",
    code = 200,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Person])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "integer", paramType = "path")))
  @ApiResponses(Array(
    new ApiResponse(
      code = 200,
      message = "true if deleted false otherwise",
      response = classOf[Boolean]),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def deletePersonRoute: Route = path("person" / IntNumber) { id =>
    delete {
      withJwtAuthorization { roles =>
        if (roles.hasAdminRole) {
          onComplete(deletePerson(id).mapTo[Boolean]) {
            case Success(wasApplied) =>
              if (wasApplied)
                complete(SimpleResponse(s"Person $id has been deleted"))
              else
                reject(PersonRejection(s"Person not found id: $id"))
            case Failure(th) => failWith(th)
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/person")
  @ApiOperation(
    value = "insert a new person",
    nickname = "insertion",
    httpMethod = "POST",
    code = 201,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Person])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(required = true, dataType = "ws.gmax.model.Person", paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(
      code = 201,
      message = "successfully inserted person",
      response = classOf[ws.gmax.model.Person],
      responseHeaders = Array(
        new ResponseHeader(name = "Location", description = "The URI of created resource", response = classOf[String]),
      )),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def insertPersonRoute: Route = path("person") {
    post {
      withJwtAuthorization { roles =>
        if (roles.hasWriteRole) {
          entity(as[Person]) { person =>
            onComplete(insertPerson(person).mapTo[Boolean]) {
              case Success(wasApplied) =>
                if (wasApplied)
                  extractRequest { request =>
                    respondWithHeaders(Location(s"${request.uri}/${person.id}")) {
                      complete((StatusCodes.Created, person))
                    }
                  }
                else
                  reject(PersonRejection(s"Insert $person failed"))
              case Failure(th) => failWith(th)
            }
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  @Path("/person")
  @ApiOperation(
    value = "updating an existing person",
    nickname = "updating",
    httpMethod = "PUT",
    code = 200,
    authorizations = Array(new Authorization(value = SwaggerDoc.securityDefinitionKey, scopes =
      Array(new AuthorizationScope(scope = SwaggerDoc.scope, description = SwaggerDoc.scopeDescription)))),
    response = classOf[ws.gmax.model.Person])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(required = true, dataType = "ws.gmax.model.Person", paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(
      code = 200,
      message = "updated person",
      response = classOf[ws.gmax.model.Person]),
    new ApiResponse(
      code = 404,
      message = "Not found",
      response = classOf[String]),
    new ApiResponse(
      code = 403,
      message = "Forbidden",
      response = classOf[String])))
  def updatePersonRoute: Route = path("person") {
    put {
      withJwtAuthorization { roles =>
        if (roles.hasWriteRole) {
          entity(as[Person]) { person =>
            onComplete(updatePerson(person).mapTo[Boolean]) {
              case Success(wasApplied) =>
                if (wasApplied)
                  complete(person)
                else
                  reject(PersonRejection(s"Update $person failed"))
              case Failure(th) => failWith(th)
            }
          }
        } else {
          reject(AuthorizationFailedRejection)
        }
      }
    }
  }

  val testRoutes: Route =
    path("rejection") {
      extractRequest {
        request =>
          reject(PersonRejection(s"Request has been rejected ${request.toString}"))
      }
    } ~ path("exception") {
      extractRequest {
        request =>
          throw new RuntimeException(s"Request ended with exception ${request.toString}")
      }
    }

  val apiRoutes: Route = personRoute ~ personQueryRoute ~
    personsRoute ~ deletePersonRoute ~ insertPersonRoute ~
    updatePersonRoute ~ testRoutes
}
