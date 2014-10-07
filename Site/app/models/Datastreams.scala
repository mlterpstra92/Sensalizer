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

abstract case class Datastreams extends CassandraTable[Datastreams, Datastream]{
  object feedID extends IntColumn(this) with PartitionKey[Int]
  object streamID extends StringColumn(this) with Index[String]
  object currentValue extends FloatColumn(this)
  object insertionTime extends DateTimeColumn(this) with PrimaryKey[DateTime] with ClusteringOrder[DateTime] with Descending
  object seqNo extends IntColumn(this) with PrimaryKey[Int] with ClusteringOrder[Int] with Descending

}
object Datastreams extends Datastreams with MyDBConnector {
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
      .value(_.seqNo, ds.seqNo)
      .ttl(10.minutes.inSeconds) // you can use TTL if you want to.
      .future()
  }

  override def fromRow(r: Row): Datastream = Datastream(feedID(r), streamID(r), seqNo(r), currentValue(r), insertionTime(r));
  // now you have the full power of Cassandra in really cool one liners.
  // The future will do all the heavy lifting for you.
  // If there is an error you get a failed Future.
  // If there is no matching record you get a None.
  // The "one" method will select a single record, as it's name says.
  // It will always have a LIMIT 1 in the query sent to Cassandra.
  // select.where(_.id eqs UUID.randomUUID()).one() translates to
  // SELECT * FROM my_custom_table WHERE id = the_id_value LIMIT 1;
  def getDatastreamIDs(feedID: Int): Future[Seq[String]] = {
    select(_.streamID).where(_.feedID eqs feedID).fetch()
  }

  def getDataValueByStreamID(feedID: Int, streamID: String): Future[Seq[Float]] = {
    select(_.currentValue).where(_.feedID eqs feedID).and(_.streamID eqs streamID).fetch()
  }

  def getLatestInsertionTime(feedID: Int): Future[Option[DateTime]] = {
    select(_.insertionTime).where(_.feedID eqs feedID).one()
  }



  /*
      // Because you are using a partition key, you can successfully using ordering
      // And you can pagina}te your records.
      // That's it, a really cool one liner.
      // The fetch method will collect an asynchronous lazy iterator into a Seq.
      // It's a good way to avoid boilerplate when retrieving a small number of items.
      def getDatastreamsPage(start: UUID, limit: Int): ScalaFuture[Seq[Datastream]] = {
        select.where(_.id gtToken start).limit(limit).fetch()



      // The fetchEnumerator method is the real power behind the scenes.
      // You can retrieve a whole table, even with billions of records, in a single query.
      // Phantom will collect them into an asynchronous, lazy iterator with very low memory foot print.
      // Enumerators, iterators and iteratees are based on Play iteratees.
      // You can keep the async behaviour or collect through the Iteratee.
      def getEntireTable: ScalaFuture[Seq[Datastream]] = {
        select.fetchEnumerator() run Iteratee.collect()
      }


      // com.websudos.phantom supports a few more Iteratee methods.
      // However, if you are looking to guarantee ordering and paginate "the old way"
      // You need an OrderPreservingPartitioner.
      def getDatastreamPage(start: Int, limit: Int): ScalaFuture[Iterator[Datastream]] = {
        select.fetchEnumerator() run Iteratee.slice(start, limit)
      }


      // Updating records is also really easy.
      // Updating one record is done like this
      def updateDatastreamAuthor(id: UUID, author: String): ScalaFuture[ResultSet] = {
        update.where(_.id eqs id).modify(_.author setTo author).future()
      }

      // Updating records is also really easy.
      // Updating multiple fields at the same time is also easy.
      def updateDatastreamAuthorAndName(id: UUID, name: String, author: String): ScalaFuture[ResultSet] = {
        update.where(_.id eqs id).modify(_.name setTo name)
          .and(_.author setTo author)
          .future()
      }

      // Deleting records has the same restrictions and selecting.
      // If something is non primary, you cannot have it in a where clause.
      def deleteDatastreamById(id: UUID): ScalaFuture[ResultSet] = {
        delete.where(_.id eqs id).future()
      }*/
}