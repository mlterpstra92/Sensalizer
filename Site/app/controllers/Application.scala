package controllers

import models.Feed
import models.Authorization
import models.Login
import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    if (models.Login.loggedIn(0))
      Ok(views.html.index(0))
    else
      Ok(views.html.promo())
  }


  def profile(userID: Long) = Action {
    if (models.Authorization.isAuthorized(userID))
      Ok(views.html.profile(userID))
    else
      Unauthorized(views.html.unauthorized())
  }

  def feed(feedID: Long) = Action{
    if (models.Authorization.isAuthorized(0))
      Ok(views.html.feed(feedID))
    else
      Unauthorized(views.html.unauthorized())
  }
  def feeds = Action{
    if (models.Authorization.isAuthorized(0))
      Ok(views.html.feeds(Feed.getList))
    else
      Unauthorized(views.html.unauthorized())
  }
}