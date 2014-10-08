package controllers

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
  val queue: mutable.PriorityQueue[Datastream] = new mutable.PriorityQueue[Datastream]();

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

  def feed(feedID: Int) = Action.async {
    models.Datastreams.getDatastreamIDs(feedID).map(res => Ok(createJsonFromDatastreams(feedID, res)).as("application/json"))
  }



  def feeds(userID: Int) = Action.async {
    //Create tables
    //Await.result(models.Feeds.createTable, 5000 millis)
    //Await.result(models.Datastreams.createTable, 5000 millis)
    //Await.result(models.Userstates.createTable, 5000 millis)
    Feeds.getList.map(list => Ok(views.html.feeds(list, models.Login.getLoggedInUser(userID).APIKey)))


  }

  /*while(true) {
    if (!queue.isEmpty) {
      val item = queue.dequeue()
      Await.result(models.Datastreams.insertNewRecord(item), 10 seconds);
    }
  }*/

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
          queue.enqueue(new Datastream((json \ "feedID").as[String].toInt, (json \ "datastreams" \\ "id").apply(i).as[String], (json \ "datastreams" \\ "current_value").apply(i).as[String].toFloat, DateTime.parse((json \ "datastreams" \\ "at").apply(i).as[String])));
          println(queue);
        }
      }
    }

    (in,out)
  }
}