package controllers

import java.util

import com.google.common.io.BaseEncoding
import com.rabbitmq.client._
import org.fusesource.mqtt.client.{Topic, BlockingConnection, MQTT, QoS}
import com.websudos.phantom.Implicits._
import models.{Datastream, Feed, Feeds}
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


object Application extends Controller {
  //Queue connection constants
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

  //Allows the user to insert a new feed in the database
  def addFeed() = Action { implicit request =>
    //Fetch data from sent Form
    val (newFeedName, newFeedID) = newFeedForm.bindFromRequest.get
    //Define a new feed object with default constants
    val newFeed = new Feed(newFeedID, newFeedName, true, "https://api.xively.com/v2/feeds/"+newFeedID, DateTime.now(), DateTime.now(), "https://xively.com/user/Sensalizer", "1.0.0")
    Feeds.insertNewRecord(newFeed)
    //Redirect back to the main page
    //Should be able to this asynchronously but this also works
    Redirect(routes.Application.index())
  }

  def index = Action {
    //Go to default landing page if user is not logged in
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

  //This transforms a feed from the database to an initial JSON object
  //It is described in the format as Chart.js understands it
  //This way, we can initialize the graph by just sending this message to it
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

  //Creates a JSON message for a new point
  //This message can be added directly to the graph by calling
  //Chart.js.addPoint(json)
  def createJson(label: String, value: Float): String = {
    Json.stringify(Json.toJson(
      Map(
        "label" -> Json.toJson(label),
        "currentValue" -> Json.toJson(value)
      )
    ))
  }

  //Set MQTT defaults for fetching live data from Xively
  def getMQTT(feedID: String, apiKey: String): MQTT = {
    val mqtt = new MQTT()

    mqtt.setHost("api.xively.com", 1883)
    mqtt.setUserName(apiKey)
    mqtt.setCleanSession(false)
    mqtt.setClientId(java.util.UUID.randomUUID().toString)
    mqtt
  }

  //Suger coated syntax for less boilerplate when interaction with MQTT
  def withIt(f: BlockingConnection => Unit)(implicit mqtt: MQTT) {
    val connection = mqtt.blockingConnection()
    connection.connect()
    f(connection)
    connection.disconnect()
  }

  //This function is called asynchronously from JQuery
  //And it is what drives the live communication
  //It fetches a feed from Cassandra, transforms it into an initial message
  // (see createJsonFromDatastreams) and sends it to the client via RabbitMQ
  //Once that has happened, it fetches live data from Xively via MQTT
  //Transforms it into relevant data (see createJson) and sends it continuously
  //to the client via RabbitMQ. It also places new data in Cassandra
  def triggerFeed = Action { request =>
    var feedIDStr: String = null
    var apiKeyStr: String = null
    var clientGUID: String = null
    //validate sent data
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

        //Get relevant queue to calling client
        val queueName = channel.queueDeclare().getQueue
        channel.queueBind(queueName, "amq.topic", clientGUID)


        //Fetch feed information from database and send it to the client
        val labels = Await.result(models.Datastreams.getDatastreamIDs(feedIDStr.toInt), 10 seconds).distinct.toList
        val dataValues: util.ArrayList[List[Float]] = new util.ArrayList[List[Float]]
        labels.map(label => {
          val List: List[Float] = Await.result(models.Datastreams.getDataValueByStreamID(feedIDStr.toInt, label), 10 seconds).toList
          dataValues.add(List)
        })
        val timestamps = Await.result(models.Datastreams.getInsertionTimes(feedIDStr.toInt), 10 seconds).toList.distinct
        println(createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps))
        channel.basicPublish("amq.topic", clientGUID, null, createJsonFromDatastreams(feedIDStr.toInt, labels, dataValues, timestamps).getBytes)

      case None => throw new Exception("Invalid form data: No form data")
    }

    println("Pushed db messages")
    //create a connection to Xively
    implicit val mqtt = getMQTT(feedIDStr, apiKeyStr)
    withIt(conn => {
      println(feedIDStr)
      conn.subscribe(Array(new Topic("/v2/feeds/" + feedIDStr, QoS.EXACTLY_ONCE)))
      //Continously fetch messages from Xively, transform it to JSON data points
      //And sends it to the client
      while (conn.isConnected && channel.isOpen) {
        val message = conn.receive()
        val msg = new String(message.getPayload)
        message.ack()
        val json: JsValue = Json.parse(msg)
        println(json)
        val label = DateTime.parse((json \ "updated").as[String])
        //Transform incoming JSON data to our JSON format
        val newJson = Json.stringify(Json.toJson(
          Map(
            "label" -> Json.toJson(label),
            "feedID" -> Json.toJson(feedIDStr),
            "streamid" -> Json.toJson((json \ "datastreams" \\ "id").map(v=>v.as[String])),
            "current_value" -> Json.toJson((json \ "datastreams" \\ "current_value").map(v=>v.as[String].toFloat))
          )
        ))
        //Insert new data in Cassandra
        for (i <- 0 until (json \ "datastreams" \\ "id").length) {
          val ds = new Datastream(feedIDStr.toInt,
            (json \ "datastreams" \\ "id").map(v => v.as[String]).apply(i),
            (json \ "datastreams" \\ "current_value").map(v => v.as[String]).apply(i).toFloat,
            label)
          models.Datastreams.insertNewRecord(ds)
        }
        //Send to client
        channel.basicPublish("amq.topic", clientGUID, null, newJson.getBytes)
      }
    })

    Ok("");
  }

  //View a feed. Fetches all data from database and returns plain text
  def feed(feedID: Int) = Action {
    val q = Await.result(models.Datastreams.getDatastreamIDs(feedID), 10 seconds)
    Ok("Feed " + feedID + ": \n" + q.mkString("\n"))
  }

  //Fetch average data for each streamid from a feed
  def getAverages(feedID: Int) = Action {
    Ok(models.Statistics.getAverageDataStreamValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n"))
  }
  //Fetch extreme values for each streamid from a feed
  def getMinMax(feedID: Int) = Action {
    val min = models.Statistics.getminimumValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n")
    val max = models.Statistics.getMaximumValues(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n")
    Ok("Minimal:\n %s\n\nMaximum:\n %s".format(min, max))
  }

  //Perform a period detection for each streamid from a feed
  //Period detection by taking the average of the average distance of max values of a streamid
  //and the average distance of min values of a streamid
  def getPeriods(feedID: Int) = Action {
    Ok(models.Statistics.getPeriods(feedID).map(q => "%s: %s".format(q._1, q._2)).mkString("\n"))
  }

  //Get all datastreams that are currently above a threshold
  //FeedIDpack is a base64 encoded value of the format "feedID-threshold"
  //because the Play! framework complains when it is passed here "as-is"
  //because of illegal characters (but fails to tell which one).
  def getAboveThreshold(feedIDpack: String) = Action {
    val items = new String(BaseEncoding.base64().decode(feedIDpack).map(_.toChar)).split("-")
    Ok(models.Statistics.getAboveThreshold(items.apply(0).toInt, items.apply(1).toFloat).mkString("\n"))
  }

  //Get an overview of all feeds an user may watch
  def feeds(userID: Int) = Action.async {
    //Create tables
    //Await.result(models.Feeds.createTable, 5000 millis)
    //Await.result(models.Datastreams.createTable, 5000 millis)
    //Await.result(models.Userstates.createTable, 5000 millis)
    Feeds.getList.map(list => Ok(views.html.feeds(list, models.Login.getLoggedInUser(userID).APIKey)))
  }
}