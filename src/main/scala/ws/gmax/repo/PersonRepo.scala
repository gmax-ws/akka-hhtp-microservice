package ws.gmax.repo

import com.datastax.driver.core.{Row, Session}
import ws.gmax.model._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class PersonRepo(session: Session, isAsync: Boolean)(implicit ec: ExecutionContext) extends Java2ScalaFuture(isAsync) {

  private val getPersonStatement = session.prepare("select id, name, address, age from persons where id=:id")
  private val getPersonsStatement = session.prepare("select id, name, address, age from persons")
  private val insertPersonStatement = session.prepare("insert into persons (id, name, address, age) values (:id, :name, :address, :age) if not exists")
  private val updatePersonStatement = session.prepare("update persons set name=:name, address=:address, age=:age where id=:id if exists")
  private val deletePersonStatement = session.prepare("delete from persons where id=:id if exists")

  type Id = Int

  def getPerson(id: Id): Future[Option[Person]] = {
    val statement = getPersonStatement.bind()
    statement.setInt("id", id)
    session.executeAsync(statement).asScalaFuture map { resultSet =>
      Option(resultSet.one()) match {
        case Some(row) => Some(toPerson(row))
        case None => None
      }
    }
  }

  def getPersons: Future[Persons] = {
    val statement = getPersonsStatement.bind()
    session.executeAsync(statement).asScalaFuture map { resultSet =>
      Persons(resultSet.all().asScala.toList map toPerson)
    }
  }

  def insertPerson(person: Person): Future[Boolean] = {
    val statement = insertPersonStatement.bind()
    statement.setInt("id", person.id)
      .setString("name", person.name)
      .setString("address", person.address)
      .setInt("age", person.age)
    session.executeAsync(statement).asScalaFuture map {
      _.wasApplied()
    }
  }

  def updatePerson(person: Person): Future[Boolean] = {
    val statement = updatePersonStatement.bind()
    statement.setInt("id", person.id)
      .setString("name", person.name)
      .setString("address", person.address)
      .setInt("age", person.age)
    session.executeAsync(statement).asScalaFuture map {
      _.wasApplied()
    }
  }

  def deletePerson(id: Id): Future[Boolean] = {
    val statement = deletePersonStatement.bind()
    statement.setInt("id", id)
    session.executeAsync(statement).asScalaFuture map {
      _.wasApplied()
    }
  }

  def toPerson(row: Row): Person = Person(row.getInt("id"),
    row.getString("name"),
    row.getString("address"),
    row.getInt("age"))
}

object PersonRepo {
  def apply(session: Session, isAsync: Boolean = true)(implicit ec: ExecutionContext): PersonRepo =
    new PersonRepo(session, isAsync)
}