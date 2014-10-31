package models

import org.apache.spark.sql.SchemaRDD
import org.apache.spark.streaming.{StreamingContext, Duration}
import org.apache.spark.{SparkContext,SparkConf}
import com.datastax.spark.connector.streaming._
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.joda.time._
import org.joda.time.DateTime


case class Statistics(feedID: Int) {

}
object Statistics {
  val conf = new SparkConf(true).set("spark.cassandra.connection.host", "54.171.159.183")
  .set("cassandra.connection.host", "54.171.159.183")
  val sc = new SparkContext("local[2]", "sensalizer", conf)
  val ssc = new StreamingContext(sc, Duration.apply(2000))
  val cc = new CassandraSQLContext(sc)
  val data = null

  def getAverageDataStreamValues(feedID: Int): Array[(String, Float)] = {
    println("called")
    //println(sc.parallelize(Array(1.0 ,4.0, 234.0, 1.0, 1.04)).reduce(_ + _)/5.0)
    //val rdd: SchemaRDD = cc.sql("SELECT * from sensalizer.datastreams WHERE feedid = ? ")
    //rdd.collect().foreach(println)

    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, {val q = u._2.map(k => k.getFloat("currentvalue")); q.sum/q.size})).collect()
    //Array(("asdf", 2.0f))
  }

  def getMaximumValues(feedID: Int): Array[(String, Float)] = {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).max)).collect()

  }
  def getminimumValues(feedID: Int): Array[(String, Float)] = {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).min)).collect()
  }

  def getSinusPeriod: Float = {
    val sinVals = for (x <- 1.0 to 100.0 by 0.1) yield (x.round, math.round(math.sin(x) * 100).toInt / 100.0f)
    var maxValue = -10000000.0
    var values:  Array[Float] = Array()
    for (item  <- sinVals) {
      maxValue = math.max(maxValue, item._2)
    }
    // Select values that have maximum value
    for (item <- sinVals)
    {
      if (item._2 == maxValue) {
        values ++= Array(item._1.toFloat)
      }
    }
    //values.foreach(println)
    // take average of distance
    val q = values.zip(values.drop(1)).map(a => math.abs(a._1 - a._2))
    q.sum / q.length.toFloat
    //(sc.parallelize(values).reduce((a, b) => math.abs(a - b)) / (values.length - 1.0f)).toFloat
    //1.0f
  }

  def getPeriods(feedID: Int): Array[(String, Float)] = {
    // Try to find a period by taking the average value of distance between (max) peaks
    val sets = ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).collect()
    val retList: Array[(String, Float)] = Array()
    for (row <- sets)
    {
      val timeLists: Array[Int] = Array()
      var maxValue = -10000000.0f
      // Determine maximum value
      for (values <- row._2)
        maxValue = math.max(maxValue, values.getFloat("currentvalue"))

      // Select values that have maximum value
      for (item <- row._2)
      {
        if (item.getFloat("currentvalue") == maxValue)
          timeLists :+ item.getDateTime("insertiontime").getMinuteOfDay
      }
      // take average of distance
      retList :+ (row._1, sc.parallelize(timeLists).reduce((a, b) => math.abs(a - b)) / (timeLists.length - 1.0f))
    }
    retList
  }

/*
  def getAboveThreshold(feedID: Int, threshold: Float) =
  {
  }*/
}
