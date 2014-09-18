package controllers

import models.Feed
import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Sensalizer"))
  }


  def profile(title: String) = Action {
    Ok(views.html.profile(title))
  }

  def feed(id: Long) = Action{
    // TODO: make sure user can view feed
    Ok(views.html.feed(id))
  }
  def feeds = Action{
    Ok(views.html.feeds(Feed.getList))
  }
}