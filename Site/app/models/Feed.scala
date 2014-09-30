package models

import java.util.UUID

import org.joda.time.DateTime
import com.twitter.conversions.time._
import scala.concurrent.{ Future => ScalaFuture }
import com.datastax.driver.core.{ResultSet,Row}
import com.websudos.phantom.Implicits._


case class Datastream(
feedID: Int,
streamID: String,
currentValue: Float,
insertionTime: DateTime)

object Datastream{}

case class Feed(
                              feedID: Int,
                              title: String,
                              priv: Boolean,
                              url: String,
                              updated: DateTime,
                              created: DateTime,
                              creator: String,
                              version: String,
                              // Store datastream IDs
                              datastreams: List[UUID]
                              )
object Feed{}

case class Userstate(
                       userID: Int,
                       username: String,
                       APIKey: String)

object Userstate{}