package controllers

import models.Feeds
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


  def profile(userID: Int) = Action {
    if (models.Authorization.isAuthorized(userID))
      Ok(views.html.profile(userID))
    else
      Unauthorized(views.html.unauthorized())
  }

  def feed(feedID: Int) = Action {
    if (models.Authorization.isAuthorized(0))
        if (feedID > 0 && models.Feeds.getList.exists(f => f.feedID == feedID))
            Ok(views.html.feed(feedID))
        else
            NotFound(views.html.notfound())
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