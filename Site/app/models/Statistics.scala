package models

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkContext,SparkConf}
import com.datastax.spark.connector._

case class Statistics(feedID: Int) {
}
object Statistics {

  val conf = new SparkConf(true)
    /*
    .set("spark.cassandra.connection.host", "54.171.11.163")
    .set("spark.eventLog.enabled", true.toString)
    .set("spark.eventLog.dir", "sparklogs")
    .setAppName("sensalizer")
    //.setMaster("local")
    .setMaster("spark://ec2-54-171-179-206.eu-west-1.compute.amazonaws.com:7077")
    .setSparkHome("/root/spark")
*/

  //val ssc = new StreamingContext(conf, Seconds(2))
  val sc = new SparkContext(conf)



  val myTable = sc.cassandraTable("sensalizer", "datastreams")


  def calcAvg(list: List[Float]): Float = {
    list.sum / list.length
  }
  def getAverageDataStreamValues(feedID: Int): Array[(String, Float)] =
  {
    //ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, calcAvg(i._2.map(z => z._2).toList))).collect()
      Array(("sadf", myTable.count().toFloat))
  }

  def getMaximumValues(feedID: Int): Array[(String ,Float)] =
  {
    //sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, i._2.map(z => z._2).toList.max)).collect()
    Array(("sadf", 234f))

  }
  def getminimumValues(feedID: Int): Array[(String ,Float)] =
  {
    //sc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).map(i => (i.getString("streamid"), i.getFloat("currentvalue"))).groupBy(_._1).map(i => (i._1, i._2.map(z => z._2).toList.min)).collect()
    Array(("sadf", 234f))

  }
}
