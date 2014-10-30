package models
import org.apache.spark.{SparkContext,SparkConf}
import com.datastax.spark.connector._


case class Statistics(feedID: Int) {
}
object Statistics {

  val conf = new SparkConf(true).set("spark.cassandra.connection.host", "54.77.184.240")
  val sc = new SparkContext("local[*]", "sensalizer", conf)

  def getAverageDataStreamValues(feedID: Int): Array[(String, Float)] = {
    sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, {val q = u._2.map(k => k.getFloat("currentvalue")); q.sum/q.size})).collect()
  }

  def getMaximumValues(feedID: Int): Array[(String, Float)] = {
    sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).max)).collect()

  }
  def getminimumValues(feedID: Int): Array[(String, Float)] = {
    sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).min)).collect()
  }

  def getPeriods(feedID: Int): Array[(String, Float)] = {
    val q = sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue", "insertiontime").groupBy(row => row.getString("streamid")).map(row => row._2)
    Array(("asdf", 2.0f))
  }
}
