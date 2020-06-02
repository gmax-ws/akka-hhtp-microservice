package ws.gmax.cassandra

import java.io.File

import com.datastax.driver.core.{Cluster, HostDistance, PoolingOptions, Session}
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.CQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper

/**
  * A trait designed to offer embedded Cassandra support
  */
trait EmbeddedCassandra {

  val DEFAULT_HOST = "127.0.0.1"
  val DEFAULT_PORT = 9042

  val tmp = System.getProperty("java.io.tmpdir")
  val base = "embeddedCassandra"

  val yamlFile = EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE
  val tmpDir = if (tmp.endsWith(File.separator)) tmp + base else tmp + File.separator + base

  /**
    * Start embedded Cassandra, connect and get a session.
    * Load test data if a data set is provided.
    *
    * @param host host default localhost (127.0.0.1)
    * @param port    port default 9042
    * @param dataSet optional data set
    * @return session
    */
  def startUp(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT, dataSet: Option[CQLDataSet] = None): Session = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(yamlFile, tmpDir)
    val cluster = Cluster.builder
      .addContactPoint(host).withPort(port)
      .withPoolingOptions(new PoolingOptions()
        .setMaxRequestsPerConnection(HostDistance.LOCAL, 4096)
        .setMaxQueueSize(1024)
        .setConnectionsPerHost(HostDistance.LOCAL, 4, 8))
      .build
    val session = cluster.connect
    loadData(session, dataSet)
    session
  }

  // initialize database
  def loadData(session: Session, dataSet: Option[CQLDataSet]) =
    dataSet foreach { data =>
      new CQLDataLoader(session).load(data)
    }

  /**
    * Clean up
    */
  def shutDown(session: Session): Unit = {
    session.close()
    session.getCluster.close()
  }
}

