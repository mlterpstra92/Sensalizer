package models

import java.util.UUID
import org.joda.time.{DateTimeComparator, DateTime}
import com.datastax.driver.core.{ResultSet, Row}
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.column.DateTimeColumn
import com.twitter.conversions.time._
import scala.concurrent.Future

case class Feed(
                 feedID: Int,
                 title: String,
                 priv: Boolean,
                 url: String,
                 updated: DateTime,
                 created: DateTime,
                 creator: String,
                 version: String
                 )
object Feed{}

abstract case class Feeds() extends CassandraTable[Feeds, Feed]{
  object feedID extends IntColumn(this) with PartitionKey[Int]{
    override lazy val name = "feedID"
  }
  object title extends StringColumn(this)
  object priv extends BooleanColumn(this)
  object url extends StringColumn(this)
  object updated extends DateTimeColumn(this)
  object created extends DateTimeColumn(this)
  object creator extends StringColumn(this)
  object version extends StringColumn(this)
}
object Feeds extends Feeds with CassandraConnector {
  // you can even rename the table in the schema to whatever you like.
  override lazy val tableName = "feeds"

  // Inserting has a bit of boilerplate on its on.
  // But it's almost always a once per table thing, hopefully bearable.
  // Whatever values you leave out will be inserted as nulls into Cassandra.
  def insertNewRecord(feed: Feed): Future[ResultSet] = {
    insert.value(_.feedID, feed.feedID)
      .value(_.title, feed.title)
      .value(_.priv, feed.priv)
      .value(_.url, feed.url)
      .value(_.updated, feed.updated)
      .value(_.created, feed.created)
      .value(_.creator, feed.creator)
      .value(_.version, feed.version)
      //.ttl(1000000.minutes.inSeconds) // you can use TTL if you want to.
      .future()
  }

  override def fromRow(r: Row): Feed = Feed(feedID(r), title(r), priv(r), url(r), updated(r), created(r), creator(r), version(r))
  // now you have the full power of Cassandra in really cool one liners.
  // The future will do all the heavy lifting for you.
  // If there is an error you get a failed Future.
  // If there is no matching record you get a None.
  // The "one" method will select a single record, as it's name says.
  // It will always have a LIMIT 1 in the query sent to Cassandra.
  // select.where(_.id eqs UUID.randomUUID()).one() translates to
  // SELECT * FROM my_custom_table WHERE id = the_id_value LIMIT 1;
  def getFeedById(feedID: Int): Future[Option[Feed]] = {
    select.where(_.feedID eqs feedID).one()
  }

  def getList: Future[Seq[Feed]] = {
    select.fetch
  }
/*
  def deleteUser(id: Int) = {
    delete.where(_.feedID eqs id).future()
  }*/
}