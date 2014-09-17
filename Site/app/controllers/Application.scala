package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Sensalizer", "Maarten"))
  }


  def profile(title: String, user: String) = Action {
    Ok(views.html.profile(title, user))
  }
}