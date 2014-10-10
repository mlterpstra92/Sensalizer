package controllers

import com.rabbitmq.client.{QueueingConsumer, ConnectionFactory, Connection, Channel}
import org.fusesource.mqtt.client.{Topic, BlockingConnection, MQTT, QoS}

import com.websudos.phantom.Implicits._
import models.{Datastream, Feed, Feeds, Datastreams}
import play.api.libs.iteratee.{Enumerator, Iteratee, Concurrent}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._


object Application extends Controller {
  val QUEUE_NAME = "sensalizer"

  val factory: ConnectionFactory = new ConnectionFactory();
  factory.setHost("54.171.108.54");
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel();

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



  def createJsonFromDatastreams(feedID: Int, datastreams: Seq[String]): String = {
    var labels = mutable.MutableList[String]()
    for (ds <- datastreams) {
      labels += ds
    }
    labels = labels.distinct

    val jsonObject = Json.toJson(
      Map(
        "labels" -> labels.map(Json.toJson(_)),
        "datasets" -> Seq(Json.toJson(labels.map { label => {

          Await.result(models.Datastreams.getDataValueByStreamID(feedID, label), 1500 millis).map(value => {
            println(value)

            Map(
              "label" -> Json.toJson(label),
              "fillColor" -> Json.toJson("rgba(0,200,200,0.0)"),
              "strokeColor" -> Json.toJson("rgba(0,200,200,1)"),
              "pointColor" -> Json.toJson("rgba(0,200,200,1)"),
              "pointStrokeColor" -> Json.toJson("#fff"),
              "pointHighlightFill" -> Json.toJson("#fff"),
              "pointHighlightStroke" -> Json.toJson("rgba(0,200,200,1)"),
              "data" -> Json.toJson(List(value))
            )
          })
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
    val data = request.body.asFormUrlEncoded match{
      case Some(map) =>
        map.get("apikey") match{
          case Some(apikey) => apiKeyStr = apikey.apply(0);
        }
        map.get("feedid") match{
          case Some(feedID) =>
            feedIDStr = feedID.apply(0)
            Await.result(models.Datastreams.getDatastreamIDs(feedID.apply(0).toInt), 2 seconds).distinct.map(label => {
              Await.result(models.Datastreams.getDataValueByStreamID(feedID.apply(0).toInt, label), 1500 millis).map(value => {
                channel.basicPublish("", QUEUE_NAME, null, createJson(label, value).getBytes);
              })
            })
        }
      case None => Application.Status(418);
    }

    println("Pushed db messages")
    implicit val mqtt = getMQTT(feedIDStr, apiKeyStr)
    println(mqtt)
    withIt(conn => {
      println("got connection")
      conn.subscribe(Array(new Topic("/v2/feeds/" + feedIDStr, QoS.AT_LEAST_ONCE)))
      println("Got topic")
      while (true) {
        val message = conn.receive()
        val msg = new String(message.getPayload)
        message.ack()
        val json: JsValue = Json.parse(msg)
        val label = DateTime.parse((json \ "updated").as[String])
        for (i <- 0 to (json \ "datastreams" \\ "id").length - 1) {
          val streamid = (json \ "datastreams" \\ "id").apply(i).as[String]
          val currentValue = (json \ "datastreams" \\ "current_value").apply(i).as[String].toFloat
          val newJson = Json.stringify(Json.toJson(
            Map(
              "label" -> Json.toJson(label),
              "feedID" -> Json.toJson(feedIDStr),
              "streamid" -> Json.toJson(streamid),
              "current_value" -> Json.toJson(currentValue)
            )
          ))
          models.Datastreams.insertNewRecord(new Datastream(feedIDStr.toInt, streamid, currentValue, label))

          channel.basicPublish("", QUEUE_NAME, null, newJson.getBytes)
        }
      }
    })

    Ok("");
  }

  def feed(feedID: Int) = Action.async {
    models.Datastreams.getDatastreamIDs(feedID).map(res =>
      Ok(createJsonFromDatastreams(feedID, res)).as("application/json")
    )
  }



  def feeds(userID: Int) = Action.async {
    //Create tables
    //Await.result(models.Feeds.createTable, 5000 millis)
    //Await.result(models.Datastreams.createTable, 5000 millis)
    //Await.result(models.Userstates.createTable, 5000 millis)
    Feeds.getList.map(list => Ok(views.html.feeds(list, models.Login.getLoggedInUser(userID).APIKey)))


  }

  def datapush =  WebSocket.using[String] { request =>
    //Concurernt.broadcast returns (Enumerator, Concurrent.Channel)

    val (out,channel) = Concurrent.broadcast[String]
    //log the message to stdout and send response back to client

    val (in) = Iteratee.foreach[String] {

      msg => {

        val json: JsValue = Json.parse(msg)
        val feedID = json \ "datastreams" \\ "id"
        for (i <- 0 to (json \ "datastreams" \\ "id").length) {
          println((json \ "datastreams" \\ "id").apply(i).as[String])
          //queue.enqueue(new Datastream((json \ "feedID").as[String].toInt, (json \ "datastreams" \\ "id").apply(i).as[String], (json \ "datastreams" \\ "current_value").apply(i).as[String].toFloat, DateTime.parse((json \ "datastreams" \\ "at").apply(i).as[String])));
          //println(queue);
        }
      }
    }

    (in,out)
  }
}