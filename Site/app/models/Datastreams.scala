package models

import java.util.UUID

import com.datastax.driver.core.{ResultSet, Row}
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.column.{SetColumn, DateTimeColumn}
import com.twitter.conversions.time._
//import scala.concurrent.duration._
import com.websudos.phantom.iteratee.Iteratee
import org.joda.time.DateTime
import scala.concurrent.{Await, Future}


case class Datastream(
                       feedID: Int,
                       streamID: String,
                       currentValue: Float,
                       insertionTime: DateTime)

object Datastream {
  implicit def dateTimeOrdering[A <: Datastream]: Ordering[A] = Ordering.fromLessThan(_.insertionTime isBefore _.insertionTime)
}

abstract case class Datastreams() extends CassandraTable[Datastreams, Datastream]{
  object feedID extends IntColumn(this) with PartitionKey[Int]
  object streamID extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Descending
  object currentValue extends FloatColumn(this)
  object insertionTime extends DateTimeColumn(this) with PrimaryKey[DateTime] with ClusteringOrder[DateTime] with Descending

}
object Datastreams extends Datastreams with CassandraConnector {
  // you can even rename the table in the schema to whatever you like.
  override lazy val tableName = "datastreams"

  // Inserting has a bit of boilerplate on its on.
  // But it's almost always a once per table thing, hopefully bearable.
  // Whatever values you leave out will be inserted as nulls into Cassandra.
  def insertNewRecord(ds: Datastream): Future[ResultSet] = {

    insert
      .value(_.feedID, ds.feedID)
      .value(_.streamID, ds.streamID)
      .value(_.currentValue, ds.currentValue)
      .value(_.insertionTime, ds.insertionTime)
      .ttl(10.minutes.inSeconds) // you can use TTL if you want to.
      .future()
  }

  override def fromRow(r: Row): Datastream = Datastream(feedID(r), streamID(r), currentValue(r), insertionTime(r))
  // now you have the full power of Cassandra in really cool one liners.
  // The future will do all the heavy lifting for you.
  // If there is an error you get a failed Future.
  // If there is no matching record you get a None.
  // The "one" method will select a single record, as it's name says.
  // It will always have a LIMIT 1 in the query sent to Cassandra.
  // select.where(_.id eqs UUID.randomUUID()).one() translates to
  // SELECT * FROM my_custom_table WHERE id = the_id_value LIMIT 1;
  def getDatastreamIDs(feedID: Int): Future[Seq[String]] = {
    select(_.streamID).allowFiltering().where(_.feedID eqs feedID).fetch()
  }

  def getDataValueByStreamID(feedID: Int, streamID: String): Future[Seq[Float]] = {
    select(_.currentValue).where(_.feedID eqs feedID).and(_.streamID eqs streamID).fetch()
  }

  def getInsertionTimes(feedID: Int): Future[Seq[DateTime]] = {
    select(_.insertionTime).where(_.feedID eqs feedID).fetch
  }
}