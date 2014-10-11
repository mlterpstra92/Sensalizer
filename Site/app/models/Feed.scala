package models

import java.util.{Comparator, UUID}

import org.joda.time.{DateTimeComparator, DateTime}
import com.twitter.conversions.time._
import scala.concurrent.{ Future => ScalaFuture }
import com.datastax.driver.core.{ResultSet,Row}
import com.websudos.phantom.Implicits._


case class Datastream(
feedID: Int,
streamID: String,
currentValue: Float,
insertionTime: DateTime)

object Datastream {
  implicit def dateTimeOrdering[A <: Datastream]: Ordering[A] = Ordering.fromLessThan(_.insertionTime isBefore _.insertionTime)
}

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

case class Userstate(
                       userID: Int,
                       username: String,
                       APIKey: String)

object Userstate{}