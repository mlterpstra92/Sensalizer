package models

import play.api.mvc.AsyncResult

import scala.concurrent._
import com.websudos.phantom.Implicits._
import scala.concurrent.duration._


/**
 * Created by maarten on 9/18/14.
 */
case class Login(userID: Int)
object Login {
  def loggedIn(userID: Int): Boolean = {
    getLoggedInUser(userID) != null
  }

  def getLoggedInUser(userID: Int): Userstate = {
    val endResult = Userstates.getUserByUserID(userID).map {
      case Some(result) => result
      case None => return null
    }
    Await.result(endResult, 500 millis)
  }
}
