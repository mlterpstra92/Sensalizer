package models

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.SparkConf
import com.datastax.spark.connector.streaming._

case class Statistics(feedID: Int) {
}
object Statistics {

  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", MyDBConnector.ip)
    .setAppName("sensalizer")
    .setMaster("local")
    //.setMaster("spark://ec2-54-171-55-61.eu-west-1.compute.amazonaws.com:7077 ")
    .setSparkHome("/home/maarten/Downloads/spark-1.1.0-bin-hadoop2.4")


  val ssc = new StreamingContext(conf, Seconds(2))
  //val myTable = sc.cassandraTable("sensalizer", "datastreams")


  def calcAvg(list: List[Float]): Float = {
    list.sum / list.length
  }
  def getAverageDataStreamValues(feedID: Int): Array[(String, Float)] =
  {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, calcAvg(i._2.map(z => z._2).toList))).collect()

  }

  def getMaximumValues(feedID: Int): Array[(String ,Float)] =
  {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, i._2.map(z => z._2).toList.max)).collect()

  }
  def getminimumValues(feedID: Int): Array[(String ,Float)] =
  {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, i._2.map(z => z._2).toList.min)).collect()

  }
}
