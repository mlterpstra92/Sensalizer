package models

import com.datastax.driver.core.{ResultSet, Row}
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.column.{SetColumn, DateTimeColumn}
import com.twitter.conversions.time._

import com.websudos.phantom.iteratee.Iteratee
import scala.concurrent.Future
case class Userstate(
                      userID: Int,
                      username: String,
                      APIKey: String)

object Userstate{}

abstract case class Userstates() extends CassandraTable[Userstates, Userstate]{
  object userID extends IntColumn(this) with PartitionKey[Int]{
    override lazy val name = "userID"
  }
  object username extends StringColumn(this)
  object APIkey extends StringColumn(this)
}
object Userstates extends Userstates with CassandraConnector {
  // you can even rename the table in the schema to whatever you like.
  override lazy val tableName = "userstates"

  // Inserting has a bit of boilerplate on its on.
  // But it's almost always a once per table thing, hopefully bearable.
  // Whatever values you leave out will be inserted as nulls into Cassandra.
  def insertNewRecord(us: Userstate): Future[ResultSet] = {
    insert.value(_.userID, us.userID)
      .value(_.username, us.username)
      .value(_.APIkey, us.APIKey)
      //.ttl(null) // you can use TTL if you want to.
      .future()
  }

  override def fromRow(r: Row): Userstate = Userstate(userID(r), username(r), APIkey(r))
  // now you have the full power of Cassandra in really cool one liners.
  // The future will do all the heavy lifting for you.
  // If there is an error you get a failed Future.
  // If there is no matching record you get a None.
  // The "one" method will select a single record, as it's name says.
  // It will always have a LIMIT 1 in the query sent to Cassandra.
  // select.where(_.id eqs UUID.randomUUID()).one() translates to
  // SELECT * FROM my_custom_table WHERE id = the_id_value LIMIT 1;
  def getUserByUserID(userID: Int): Future[Option[Userstate]] = {
    select.where(_.userID eqs userID).one()
  }

  def getUsers: Future[Seq[Userstate]] = {
    select.fetchEnumerator() run Iteratee.collect()
  }

  def deleteUser(id: Int) = {
    delete.where(_.userID eqs id).future()
  }
}