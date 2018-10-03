package ws.gmax

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.datastax.driver.core.Session
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import ws.gmax.Global._
import ws.gmax.actor.{EmbeddedCassandraActor, PersonSupervisorActor}
import ws.gmax.cors.Cors
import ws.gmax.model._
import ws.gmax.routes.{PersonJsonProtocol, PersonRejection, PersonServiceRoutes, SimpleResponse}
import ws.gmax.service.PersonService
import ws.gmax.swagger.SwaggerDoc

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

/** Global settings */
object Global {
  implicit val system: ActorSystem = ActorSystem("person-services")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)

  val config: Config = system.settings.config.getConfig("app")
  val port: Int = config.getInt("http-port")
  val withCorsFilter: Boolean = config.getBoolean("withCorsFilter")
  val localhostAuth: Boolean = config.getBoolean("localhostAuth")
}

/** Global exception handler */
sealed trait ExceptionHandling extends PersonJsonProtocol {

  val exceptionHandler = ExceptionHandler {
    case th: Throwable => complete((InternalServerError, SimpleResponse(th.getMessage)))
  }
}

/** Global rejection handler */
sealed trait RejectionHandling extends PersonJsonProtocol {

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case MissingCookieRejection(cookieName) =>
      complete((BadRequest, SimpleResponse(s"No cookies, $cookieName no service!!!")))
    }
    .handle { case AuthorizationFailedRejection =>
      complete((Forbidden, SimpleResponse("You're out of your depth!")))
    }
    .handle { case PersonRejection(message) =>
      complete((NotFound, SimpleResponse(s"$message")))
    }
    .handle { case ValidationRejection(message, _) =>
      complete((InternalServerError, SimpleResponse(s"That wasn't valid! $message")))
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete((MethodNotAllowed, SimpleResponse(s"Can't do that! Supported: ${names mkString " or "}!")))
    }
    .handleNotFound {
      complete((NotFound, SimpleResponse("Not here!")))
    }
    .result()
}

sealed trait CassandraActor {
  private val dataSet = CassandraDataSet("movie-test.cql", "movie")
  private val cassandraSettings = CassandraSettings(port = 9142, dataSet = dataSet)
  private val cassandraActor = system.actorOf(EmbeddedCassandraActor(cassandraSettings),
    "embeddedCassandraActor")

  private def send(message: CassandraMessage): Future[Any] = {
    cassandraActor ? message
  }

  def startUpCassandra: Future[Either[Throwable, Session]] =
    send(StartupCassandra).mapTo[Either[Throwable, Session]]

  def shutDownCassandra(session: Session): Future[Any] = send(ShutdownCassandra(session))
}

sealed trait AppTrait extends ExceptionHandling with RejectionHandling with
  CassandraActor with Cors with LazyLogging {

  def shutdownAndExit(code: Int): Unit = {
    system.terminate()
    System.exit(1)
  }

  def startHttpServer(routes: Route): Future[Http.ServerBinding] = {
    val httpServer = Http().bindAndHandle(routes, "0.0.0.0", Global.port)
    println(s"HTTP server is ready http://localhost:${Global.port}/${system.name}/swagger")
    println("Press RETURN to stop...")
    httpServer
  }

  def stopHttpServer(httpServer: Future[Http.ServerBinding], session: Session): Unit = {
    logger.info("Shutting down HTTP server...")
    httpServer
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ ⇒
      logger.info("HTTP server is down")
      logger.info("Shutting down Cassandra database...")
      shutDownCassandra(session) onComplete { _ =>
        logger.info("Cassandra database is down, exit")
        shutdownAndExit(0)
      }
    }
  }

  def startupApp(session: Session): Unit = {
    /** Create other actors */
    val personsSupervisorActor: ActorRef = system.actorOf(PersonSupervisorActor(session),
      "personSupervisorActor")

    val handler: Route = withCorsHandler(withCorsFilter) {
      PersonService(personsSupervisorActor, localhostAuth).handler
    }

    /** routes */
    val routes: Route = handleExceptions(exceptionHandler) {
      SwaggerDoc(Seq(classOf[PersonServiceRoutes])) ~ handler
    }

    val httpServer: Future[Http.ServerBinding] = startHttpServer(routes)
    StdIn.readLine() // let it run until user presses return
    stopHttpServer(httpServer, session)
  }
}

object PersonServiceBoot extends App with AppTrait {

  startUpCassandra onComplete {
    case Failure(th) =>
      logger.error("Cassandra startup failed", th)
      shutdownAndExit(1)
    case Success(result) =>
      result match {
        case Left(th) =>
          logger.error("Cannot get Cassandra session", th)
          shutdownAndExit(2)
        case Right(session) => startupApp(session)
      }
  }
}
