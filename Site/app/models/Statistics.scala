package models

import org.apache.spark.{SparkContext, SparkConf}
import com.datastax.spark.connector._

case class Statistics(feedID: Int) {
}
object Statistics {

  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", MyDBConnector.ip)
    .setAppName("sensalizer")
    .setMaster("local")
    //.setMaster("spark://ec2-54-171-55-61.eu-west-1.compute.amazonaws.com:7077 ")
    .setSparkHome("/home/maarten/Downloads/spark-1.1.0-bin-hadoop2.4")


  val sc = new SparkContext(conf)
  val myTable = sc.cassandraTable("sensalizer", "datastreams")

  def calcAvg(list: Iterable[Float]): Long = {
    val s = list.toList
    (s.sum / s.length).toLong
  }
  def getAverageDataStreamValues(feedID: Int): Array[(String, Long)] =
  {
    myTable.where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, calcAvg(i._2.map(z => z._2)))).collect()

  }
}
