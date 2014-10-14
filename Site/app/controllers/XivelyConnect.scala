package controllers

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}
import controllers.Application._
import play.api._
import play.api.libs.json.{Json, JsValue}
import java.util
import org.fusesource.mqtt.client.{Topic, QoS}
import com.websudos.phantom.Implicits._
import org.joda.time.DateTime
import play.api.mvc._
import scala.concurrent.{Await}
import scala.concurrent.duration._

/**
 * Created by Stefan on 14-Oct-14.
 */
object XivelyConnect {
  def start() {
    Logger.info("Awsome!")
    triggerFeed.start
  }

  val triggerFeed = new Thread (new Runnable {
    def run() {
      println("LIFE is Live")
      while (true) {
        val feeds = Await.result(models.Feeds.getFeedIDs, 5 seconds)
        println(feeds)
        feeds.map(feed => {

          val apiKey = "ChaM9GovFEW6Q2dFuQTAVucAl4bJvJKsQNVS3pars6QDVirs"
          val labels = Await.result(models.Datastreams.getDatastreamIDs(feed), 2 seconds).distinct.toList
          val dataValues: util.ArrayList[List[Float]] = new util.ArrayList[List[Float]];
          labels.map(label => {
            val List: List[Float] = Await.result(models.Datastreams.getDataValueByStreamID(feed, label), 1500 millis).toList
            dataValues.add(List)
            println(dataValues)
          })
          val timestamps = Await.result(models.Datastreams.getInsertionTimes(feed), 2 seconds).toList.distinct
          println(createJsonFromDatastreams(feed, labels, dataValues, timestamps))
          channel.basicPublish("", QUEUE_NAME, null, createJsonFromDatastreams(feed, labels, dataValues, timestamps).getBytes())
        })
      }
    }
  })
}