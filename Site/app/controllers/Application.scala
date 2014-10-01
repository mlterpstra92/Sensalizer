package controllers


import com.websudos.phantom.Implicits._
import models.Feeds
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

object Application extends Controller {

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
    var jsonLabels = mutable.MutableList[JsValue]()
    for (ds <- datastreams) {
      labels += ds
      jsonLabels += Json.toJson(ds)
    }
    labels.distinct

    val jsonObject = Json.toJson(
      Map(
        "labels" -> jsonLabels,
        "datasets" -> Seq(Json.toJson(labels.map { label => {

          Await.result(models.Datastreams.getDataValueByStreamID(feedID, label), 500 millis).map(value => {
            Map(
              "label" -> Json.toJson(label),
              "fillColor" -> Json.toJson("rgba(0,200,200,0.4)"),
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

  def feed(feedID: Int) = Action {
    Await.result(models.Datastreams.getDatastreamIDs(feedID).map(res => {
      Ok(createJsonFromDatastreams(feedID, res)).as("application/json")
    }), 500 millis)
  }



  def feeds = Action{
    //Create tables
    //Await.result(models.Feeds.createTable, 5000 millis)
    //Await.result(models.Datastreams.createTable, 5000 millis)
    //Await.result(models.Userstates.createTable, 5000 millis)
    if (models.Authorization.isAuthorized(0)) {
      Await.result({
        Feeds.getList.map(list => Ok(views.html.feeds(list)))
      }, 500 millis)
    }
    else
      Unauthorized(views.html.unauthorized())
  }
}