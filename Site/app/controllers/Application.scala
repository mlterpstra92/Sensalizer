package controllers


import models._
import org.joda.time.DateTime
import play.api._
import play.api.mvc._
import scala.collection.mutable
import scala.concurrent._
import scala.util.{Success, Failure}
import play.api.libs.json._
import com.datastax.driver.core.Row
import com.websudos.phantom.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import com.datastax.driver.core.{ResultSet, Row}
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.Implicits._
import com.websudos.phantom.column.{SetColumn, DateTimeColumn}
import com.websudos.phantom.iteratee.Iteratee
import scala.collection.mutable.MutableList

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
    var labels = MutableList[String]();
    var jsonLabels = MutableList[JsValue]();
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
              "fillColor" -> Json.toJson("rgba(220,220,220,0.2)"),
              "strokeColor" -> Json.toJson("rgba(220,220,220,1)"),
              "pointColor" -> Json.toJson("rgba(220,220,220,1)"),
              "pointStrokeColor" -> Json.toJson("#fff"),
              "pointHighlightFill" -> Json.toJson("#fff"),
              "pointHighlightStroke" -> Json.toJson("rgba(220,220,220,1)"),
              "data" -> Json.toJson(List(value))
            )
          }
          )
        }
        }
      )
      )
    )
    )
        /*for (label <- labels) {
          var outerRes: Seq[Float] = null
          Await.result(models.Datastreams.getDataValueByStreamID(feedID, label.toString).map(res2 => {
            outerRes = res2
          }), 500 millis);

          "datasets" -> Seq(

            Json.toJson(
              Map(
                "label" -> Json.toJson(label),
                "fillColor" -> Json.toJson("rgba(220,220,220,0.2)"),
                "strokeColor" -> Json.toJson("rgba(220,220,220,1)"),
                "pointColor" -> Json.toJson("rgba(220,220,220,1)"),
                "pointStrokeColor" -> Json.toJson("#fff"),
                "pointHighlightFill" -> Json.toJson("#fff"),
                "pointHighlightStroke" -> Json.toJson("rgba(220,220,220,1)"),
                "data" -> Json.toJson(outerRes))
            )
          )
        }*/
   // print(Json.stringify(jsonObject))
    val q = Json.stringify(jsonObject)
    println(q)
    q
  }

  def feed(feedID: Int) = Action {
      Await.result(models.Datastreams.getDatastreamIDs(feedID).map(res => {
        Ok(createJsonFromDatastreams(feedID, res)).as("application/json")
      }), 500 millis);

/*      if (models.Authorization.isAuthorized(0))
        if (feedID > 0 && models.Feeds.getList.exists(f => f.feedID == feedID)){
          val jsonObject = Json.toJson(
          Map(
            "labels" -> List(Json.toJson("Eating"), Json.toJson("Drinking"), Json.toJson("Sleeping"), Json.toJson("Designing"), Json.toJson("Coding"), Json.toJson("Cycling"), Json.toJson("Running")),
              "datasets" -> Seq(
                Json.toJson(
                  Map(
                    "label" -> Json.toJson("My First dataset"),
                    "fillColor" ->Json.toJson("rgba(220,220,220,0.2)"),
                    "strokeColor" -> Json.toJson("rgba(220,220,220,1)"),
                    "pointColor" -> Json.toJson("rgba(220,220,220,1)"),
                    "pointStrokeColor" -> Json.toJson("#fff"),
                    "pointHighlightFill" -> Json.toJson("#fff"),
                    "pointHighlightStroke" -> Json.toJson("rgba(220,220,220,1)"),
                    "data" -> Json.toJson(Array(65, 59, 90, 81, 56, 55, 40))
                  )
                )
              )
            )
          )
          Ok(Json.stringify(jsonObject)).as("application/json")
        }
        else
            Unauthorized(views.html.unauthorized()) //This has to be not found or something similar
      else
        Unauthorized(views.html.unauthorized())*/
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