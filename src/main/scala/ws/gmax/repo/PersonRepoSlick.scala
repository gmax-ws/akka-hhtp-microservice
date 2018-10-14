package ws.gmax.repo

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.config.Config
import slick.jdbc.H2Profile.api._
import ws.gmax.model.{Person, Persons}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class SlackDb(config: Config) {

  val driver: String = config.getString("db.driver")
  val jdbcUrl: String = config.getString("db.jdbcUrl")
  val userName: String = config.getString("db.userName")
  val password: String = config.getString("db.password")
  val poolSize: Int = config.getInt("db.poolSize")

  /** build a connection pool DataSet */
  val ds = new ComboPooledDataSource
  ds.setDriverClass(driver)
  ds.setJdbcUrl(jdbcUrl)
  ds.setUser(userName)
  ds.setPassword(password)

  /** Build db */
  val db = Database.forDataSource(ds, maxConnections = Some(poolSize))
}

sealed trait PersonModel extends PersonRepo {

  type PersonProjection = (Id, String, String, Int)

  implicit def asPerson(p: PersonProjection): Person =
    Person(p._1, p._2, p._3, p._4)

  implicit def fromPerson(p: Person): PersonProjection =
    (p.id, p.name, p.address, p.age)

  class PersonTable(tag: Tag) extends Table[PersonProjection](tag, "PERSON") {
    def id = column[Id]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def address = column[String]("ADDRESS")

    def age = column[Int]("AGE")

    def * = (id, name, address, age)
  }

  val person = TableQuery[PersonTable]
}

class PersonRepoSlick(config: Config) extends SlackDb(config) with PersonModel {

  /** create schema */
  db.run(person.schema.create)

  def getPerson(id: Id): Future[Option[Person]] =
    db.run(person.filter(_.id === id).result.headOption) map { result =>
      result map asPerson
    }

  def getPersons: Future[Persons] =
    db.run(person.result) map { result =>
      Persons(result.toList map asPerson)
    }

  def insertPerson(p: Person): Future[Boolean] = {
    val action = person += p
    db.run(action.transactionally) map (_ == 1)
  }

  def updatePerson(p: Person): Future[Boolean] = {
    val action = person.filter(_.id === p.id).update(p)
    db.run(action.transactionally) map (_ == 1)
  }

  def deletePerson(id: Id): Future[Boolean] = {
    val action = person.filter(_.id === id).delete
    db.run(action.transactionally) map (_ == 1)
  }
}

object PersonRepoSlick {
  def apply(config: Config): PersonRepoSlick = new PersonRepoSlick(config)
}