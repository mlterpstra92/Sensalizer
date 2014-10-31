package controllers

import java.util

import com.rabbitmq.client._
import org.fusesource.mqtt.client.{Topic, BlockingConnection, MQTT, QoS}

import com.websudos.phantom.Implicits._
import models.{Datastream, Feed, Feeds, Datastreams}
import play.api.libs.iteratee.{Enumerator, Iteratee, Concurrent}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._
import models.Statistics._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.data._
import play.api.data.Forms._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


object Application extends Controller {
  val QUEUE_NAME = "sensalizer"

  val factory: ConnectionFactory = new ConnectionFactory()
  factory.setHost("54.171.159.157")
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel()
  channel.exchangeDeclare("amq.topic", "topic", true, false, null)

  case class feedData(feedID: String)

  val newFeedForm = Form(
    tuple(
      "feedName" -> nonEmptyText,
      "feedID" -> number
    )
  )

  def addFeed() = Action { implicit request =>
    val (newFeedName, newFeedID) = newFeedForm.bindFromRequest.get
    val newFeed = new Feed(newFeedID, newFeedName, true, "https://api.xively.com/v2/feeds/"+newFeedID, DateTime.now(), DateTime.now(), "https://xively.com/user/Sensalizer", "1.0.0")
    Feeds.insertNewRecord(newFeed)
    Redirect(routes.Application.index())
  }

  def index = Action {
    if (models.Login.getLoggedInUser(0) != null)
      Ok(views.html.index(0))
    else
      Ok(views.html.promo())
  }


  def profile(userID: Int) = Action {
    if (models.Authorization.isAuthorized(userID))
      Ok(views.html.profile(userID))
    else
      Unauthorized(views.html.unauthorized())
  }

  def createJsonFromDatastreams(feedID: Int, labels: Seq[String], data:util.ArrayList[List[Float]], timestamps: Seq[DateTime]): String = {
    val jsonObject = Json.toJson(
      Map(
        "labels" -> timestamps.map(Json.toJson(_)),
        "datasets" -> Seq(Json.toJson(labels.map { label => {

          Map(
            "label" -> Json.toJson(label),
            "fillColor" -> Json.toJson("rgba(0,200,200,1.0)"),
            "strokeColor" -> Json.toJson("rgba(0,200,200,1)"),
            "pointColor" -> Json.toJson("rgba(0,200,200,1)"),
            "pointStrokeColor" -> Json.toJson("#fff"),
            "pointHighlightFill" -> Json.toJson("#fff"),
            "pointHighlightStroke" -> Json.toJson("rgba(0,200,200,1)"),
            "data" -> Json.toJson(data.get(labels.indexOf(label)))
          )
        }}))
      )
    )
    Json.stringify(jsonObject)
  }

  def createJson(label: String, value: Float): String = {
    Json.stringify(Json.toJson(
      Map(
        "label" -> Json.toJson(label),
        "currentValue" -> Json.toJson(value)
      )
    ))
  }

  def getMQTT(feedID: String, apiKey: String): MQTT = {
    val mqtt = new MQTT()

    mqtt.setHost("api.xively.com", 1883)
    mqtt.setUserName(apiKey)
    mqtt.setCleanSession(false)
    mqtt.setClientId(java.util.UUID.randomUUID().toString)
    mqtt
  }


  def withIt(f: BlockingConnection => Unit)(implicit mqtt: MQTT) {
    val connection = mqtt.blockingConnection()
    connection.connect()

    f(connection)

    connection.disconnect()
  }


  def triggerFeed = Action { request =>
    var feedIDStr: String = null
    var apiKeyStr: String = null
    var clientGUID: String = null
    val data = request.body.asFormUrlEncoded match {
      case Some(map) =>
        map.get("apikey") match {
          case Some(apikey) => apiKeyStr = apikey.apply(0);
          case None => throw new Exception("Invalid form data: no APIkey")
        }
        map.get("feedid") match {
          case Some(feedID) => feedIDStr = feedID.apply(0)
          case None => throw new Exception("Invalid form data: no FeedID")
        }
        map.get("guid") match {
          case Some(guid) => clientGUID = guid.apply(0)
          case None => throw new Exception("Invalid form data: no GUID")
        }

        val queueName = channel.queueDeclare().getQueue
        channel.queueBind(queueName, "amq.topic", clientGUID)



        val labels = Await.result(models.Datastreams.getDatastreamIDs(feedIDStr.toInt), 2 seconds).distinct.toList
        val dataValues: util.ArrayList[List[Float]] = new util.ArrayList[List[Float]]
        labels.map(label => {
          val List: List[Float] = Await.result(models.Datastreams.getDataValueByStreamID(feedIDStr.toInt, label), 1500 millis).toList
          dataValues.add(List)
        })
        val timestamps = Await.result(models.Datastreams.getInsertionTimes(feedIDStr.toInt), 2 seconds).toList.distinct
        println(createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps))
        channel.basicPublish("amq.topic", clientGUID, null, createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps).getBytes)

      case None => throw new Exception("Invalid form data: No form data")
    }

    println("Pushed db messages")
    implicit val mqtt = getMQTT(feedIDStr, apiKeyStr)
    withIt(conn => {
      println(feedIDStr)
      conn.subscribe(Array(new Topic("/v2/feeds/" + feedIDStr, QoS.AT_LEAST_ONCE)))
      while (true) {
        val message = conn.receive()
        val msg = new String(message.getPayload)
        message.ack()
        val json: JsValue = Json.parse(msg)
        println(json)
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
        channel.basicPublish("amq.topic", clientGUID, null, newJson.getBytes)
      }
    })

    Ok("");
  }

  def feed(feedID: Int) = Action {
    val q = Await.result(models.Datastreams.getDatastreamIDs(feedID), 2 seconds)
    Ok("Feed " + feedID + ": \n" + q.mkString("\n"))
  }

  def getAverages(feedID: Int) = Action {
    Ok(models.Statistics.getAverageDataStreamValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n"))
   // Ok("asdf")
  }
/*
  def getMinMax(feedID: Int) = Action {
    val min = models.Statistics.getminimumValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n")
    val max = models.Statistics.getMaximumValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n")
    Ok("Minimal:\n %s\n\nMaximum:\n %s".format(min, max))
  }
*/
  def getPeriods(feedID: Int) = Action {
    Ok(models.Statistics.getPeriods(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n"))
  }

  def sinPeriod = Action {
    Ok(models.Statistics.getSinusPeriod.toString)
  }
  def feeds(userID: Int) = Action.async {
    //Create tables
    //Await.result(models.Feeds.createTable, 5000 millis)
    //Await.result(models.Datastreams.createTable, 5000 millis)
    //Await.result(models.Userstates.createTable, 5000 millis)
    Feeds.getList.map(list => Ok(views.html.feeds(list, models.Login.getLoggedInUser(userID).APIKey)))


  }
}