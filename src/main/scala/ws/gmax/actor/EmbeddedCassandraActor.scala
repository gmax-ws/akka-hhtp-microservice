package ws.gmax.actor

import akka.actor.{Actor, ActorLogging, Props}
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import ws.gmax.cassandra.EmbeddedCassandra
import ws.gmax.model.{CassandraSettings, ShutdownCassandra, StartupCassandra}

import scala.util.Try

class EmbeddedCassandraActor(settings: CassandraSettings) extends Actor with ActorLogging with EmbeddedCassandra {

  val dataSet = new ClassPathCQLDataSet(settings.dataSet.dataSetLocation,
    settings.dataSet.keyspaceCreation,
    settings.dataSet.keySpaceDeletion,
    settings.dataSet.keyspaceName
  )

  override def preStart(): Unit = log.info("Cassandra database is up")

  override def postStop(): Unit = log.info("Cassandra database is down")

  override def receive: Receive = {
    case StartupCassandra =>

      val result = Try {
        val session = startUp(settings.host, settings.port, dataSet = Some(dataSet))
        session.execute("select * from system.peers")
        session
      }.toEither

      if (result.isRight)
          context.become(afterStartup)

      sender ! result
  }

  def afterStartup: Receive = {
    case ShutdownCassandra(session) =>
      Try(shutDown(session))
  }
}

object EmbeddedCassandraActor {
  def apply(settings: CassandraSettings): Props = Props(new EmbeddedCassandraActor(settings))
}