package ws.gmax.model

import com.datastax.driver.core.Session

sealed trait CassandraMessage

case object StartupCassandra extends CassandraMessage

case class ShutdownCassandra(session: Session) extends CassandraMessage

case class CassandraDataSet(dataSetLocation: String, keyspaceName: String,
                            keyspaceCreation: Boolean = true, keySpaceDeletion: Boolean = false)

case class CassandraSettings(host: String = "localhost", port: Int = 9042, dataSet: CassandraDataSet)
