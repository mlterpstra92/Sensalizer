package controllers

import models.Feeds
import models.Authorization
import models.Login
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.datastax.driver.core.Row
import com.websudos.phantom.sample.ExampleModel
import com.websudos.phantom.Implicits._



object Application extends Controller {

  def index = Action {
    if (models.Login.loggedIn(0))
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


    def feed(feedID: Int) = Action {
      if (models.Authorization.isAuthorized(0))
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
        Unauthorized(views.html.unauthorized())
    }

  def feeds = Action{
    if (models.Authorization.isAuthorized(0))
      Ok(views.html.feeds(Feeds.getList))
    else
      Unauthorized(views.html.unauthorized())
  }
}