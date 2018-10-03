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

  val tmp = System.getProperty("java.io.tmpdir")
  val base = "embeddedCassandra"

  val yamlFile = EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE
  val tmpDir = if (tmp.endsWith(File.separator)) tmp + base else tmp + File.separator + base

  /**
    * Start embedded Cassandra, connect and get a session.
    * Load test data if a data set is provided.
    *
    * @param address host default localhost (127.0.0.1)
    * @param port    port default 9042
    * @param dataSet optional data set
    * @return session
    */
  def startUp(address: String = "localhost", port: Int = 9042, dataSet: Option[CQLDataSet] = None): Session = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(yamlFile, tmpDir)
    val cluster = Cluster.builder
      .addContactPoint(address).withPort(port)
      .withPoolingOptions(new PoolingOptions()
        .setMaxRequestsPerConnection(HostDistance.LOCAL, 4096)
        .setMaxQueueSize(1024)
        .setConnectionsPerHost(HostDistance.LOCAL, 4, 8))
      .build
    val session = cluster.connect

    dataSet foreach { data =>
      new CQLDataLoader(session).load(data)
    }
    session
  }

  /**
    * Clean up
    */
  def shutDown(session: Session): Unit = {
    session.close()
    session.getCluster.close()
  }
}

