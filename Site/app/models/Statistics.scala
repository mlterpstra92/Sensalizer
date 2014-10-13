package models

import org.apache.spark.{SparkContext, SparkConf}
import com.datastax.spark.connector._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
case class Statistics(feedID: Int) {
}
object Statistics {
  val conf = new SparkConf(true)
    .set("cassandra.connection.host", MyDBConnector.ip)
    .set("spark.cassandra.connection.host", MyDBConnector.ip)


  val sc = new SparkContext("spark://ec2-54-171-55-61.eu-west-1.compute.amazonaws.com:7077", "sensalizer", conf)
  val myTable = sc.cassandraTable("sensalizer", "datastreams")


  def getAverageDataStreamValues(feedID: Int): Long =
  {

    /*println(conf.toDebugString)
    //myTable.toArray.foreach(println)
    println(MyDBConnector.ip)
    val seq: Seq[Float] = ((0 until 10) map {i => (i*i).toFloat}).toSeq
    Future(seq)*/
    myTable.count
  }
}
