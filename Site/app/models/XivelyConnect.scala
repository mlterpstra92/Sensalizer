package models

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
object XivelyConnect extends Controller{
  def start() {
    Logger.info("Awsome!")
  }

  def triggerFeed = Action { request =>
    var feedIDStr: String = null
    var apiKeyStr: String = null
    val data = request.body.asFormUrlEncoded match{
      case Some(map) =>
        map.get("apikey") match{
          case Some(apikey) => apiKeyStr = apikey.apply(0);
        }
        map.get("feedid") match{
          case Some(feedID) => {
            feedIDStr = feedID.apply(0)
            val labels = Await.result(models.Datastreams.getDatastreamIDs(feedIDStr.toInt), 2 seconds).distinct.toList
            val dataValues: util.ArrayList[List[Float]] = new util.ArrayList[List[Float]];
            labels.map(label => {
              val List: List[Float] = Await.result(models.Datastreams.getDataValueByStreamID(feedIDStr.toInt, label), 1500 millis).toList
              dataValues.add(List)
            })
            val timestamps = Await.result(models.Datastreams.getInsertionTimes(feedIDStr.toInt), 2 seconds).toList.distinct
            println(createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps))
            channel.basicPublish("", QUEUE_NAME, null, createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps).getBytes())
          }
        }
      case None => XivelyConnect.Status(418);
    }

    println("Pushed db messages")
    implicit val mqtt = getMQTT(feedIDStr, apiKeyStr)
    //println(mqtt)
    withIt(conn => {
      //println("got connection")
      conn.subscribe(Array(new Topic("/v2/feeds/" + feedIDStr, QoS.AT_LEAST_ONCE)))
      //println("Got topic")
      while (true) {
        val message = conn.receive()
        val msg = new String(message.getPayload)
        message.ack()
        val json: JsValue = Json.parse(msg)
        val label = DateTime.parse((json \ "updated").as[String])
        val newJson = Json.stringify(Json.toJson(
          Map(
            "label" -> Json.toJson(label),
            "feedID" -> Json.toJson(feedIDStr),
            "streamid" -> Json.toJson((json \ "datastreams" \\ "id").map(v=>v.as[String])),
            "current_value" -> Json.toJson((json \ "datastreams" \\ "current_value").map(v=>v.as[String].toFloat))
          )
        ))
        for (i <- 0 until (json \ "datastreams" \\ "id").length) {
          val ds = new Datastream(feedIDStr.toInt,
            (json \ "datastreams" \\ "id").map(v => v.as[String]).apply(i),
            (json \ "datastreams" \\ "current_value").map(v => v.as[String]).apply(i).toFloat,
            label)
          models.Datastreams.insertNewRecord(ds)
        }
        channel.basicPublish("", QUEUE_NAME, null, newJson.getBytes)
      }
    })

    Ok("");
  }
}
