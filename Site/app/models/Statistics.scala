package models

import org.apache.spark.sql.SchemaRDD
import org.apache.spark.streaming.{StreamingContext, Duration}
import org.apache.spark.{SparkContext,SparkConf}
import com.datastax.spark.connector.streaming._
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.joda.time._
import org.joda.time.DateTime
import org.joda.time.format.{PeriodFormatter, PeriodFormatterBuilder}


case class Statistics(feedID: Int) {

}
object Statistics {
  val conf = new SparkConf(true).set("spark.cassandra.connection.host", "54.171.159.183")
  .set("cassandra.connection.host", "54.171.159.183")
  .setSparkHome("/home/ubuntu/spark-1.1.0-bin-hadoop2.4")
  val sc = new SparkContext("local[2]", "sensalizer", conf)
  val ssc = new StreamingContext(sc, Duration.apply(2000))
  val cc = new CassandraSQLContext(sc)
  val data = null

  def getAverageDataStreamValues(feedID: Int): Array[(String, Float)] = {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, {val q = u._2.map(k => k.getFloat("currentvalue")); q.sum/q.size})).collect()
  }

  def getMaximumValues(feedID: Int): Array[(String, Float)] = {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).max)).collect()
  }
  def getminimumValues(feedID: Int): Array[(String, Float)] = {
    ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue").groupBy(row => row.getString("streamid")).map(u => (u._1, u._2.map(k => k.getFloat("currentvalue")).min)).collect()
  }

  def getPeriods(feedID: Int): Array[(String, String)] = {
    val daysHoursMinutes: PeriodFormatter = new PeriodFormatterBuilder()
      .appendDays()
      .appendSuffix(" day", " days")
      .appendSeparator(" and ")
      .appendMinutes()
      .appendSuffix(" minute", " minutes")
      .appendSeparator(" and ")
      .appendSeconds()
      .appendSuffix(" second", " seconds")
      .toFormatter

    // Try to find a period by taking the average value of distance between (max) peaks
    val sets = ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue", "insertiontime").groupBy(row => row.getString("streamid")).collect()
    var retList: Array[(String, String)] = Array()
    for (row <- sets)
    {
      var timeListsMax: Array[Long] = Array()
      var timeListsMin: Array[Long] = Array()
      val sparkData = sc.parallelize(row._2.map(q => q.getFloat("currentvalue")).toList)
      val minValue = sparkData.min()
      val maxValue = sparkData.max()

      for (item <- row._2)
      {
        val threshold = 0.01
        val currentValue = item.getFloat("currentvalue")
        if (math.abs(maxValue - currentValue) < threshold) {
          //Get date as number of milliseconds since 1970-01-01T00:00:00Z.
          timeListsMax ++= Array(item.getDateTime("insertiontime").getMillis)
        }
        else if(math.abs(currentValue - minValue) < threshold) {
          timeListsMin ++= Array(item.getDateTime("insertiontime").getMillis)
        }
      }
      timeListsMax = timeListsMax.sortWith(_ < _)
      timeListsMin = timeListsMin.sortWith(_ < _)
      // take average of distance
      val differenceListMax = sc.parallelize(timeListsMax.zip(timeListsMax.drop(1))).map(a => math.abs(a._1 - a._2)).collect()
      val differenceListMin = sc.parallelize(timeListsMin.zip(timeListsMin.drop(1))).map(a => math.abs(a._1 - a._2)).collect()

      val avgMax = differenceListMax.sum.toFloat / differenceListMax.length
      val avgMin = differenceListMin.sum.toFloat / differenceListMin.length

      //average the averages
      val avgavg = (avgMax + avgMin) / 2.0
      retList ++= Array((row._1, daysHoursMinutes.print(new Period(avgavg.toLong))))
    }
    retList
  }


  def getAboveThreshold(feedID: Int, threshold: Float): Array[String] =
  {
    val sets = ssc.cassandraTable("sensalizer", "datastreams").where("feedid = ?", feedID).select("streamid", "currentvalue", "insertiontime").groupBy(row => row.getString("streamid")).collect()
    implicit object DateTimeOrdering extends Ordering[DateTime] {
      def compare(d1: DateTime, d2: DateTime) = d1.compareTo(d2)
    }
    val latestTime = sc.parallelize(sets.apply(0)._2.toList.map(u => u.getDateTime("insertiontime"))).max()
    var retStr: Array[String] = Array[String]()
    for (item <- sets)
    {
      retStr ++= sc.parallelize(item._2.toList.filter(u => u.getDateTime("insertiontime") == latestTime).toList.filter(u => u.getFloat("currentvalue") >= threshold).map(u => u.getString("streamid"))).collect()
    }
    retStr
  }
}
