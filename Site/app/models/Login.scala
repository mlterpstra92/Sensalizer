package models

import java.util.concurrent.TimeoutException
import scala.concurrent._
import com.websudos.phantom.Implicits._
import scala.concurrent.duration._

/**
 * Created by maarten on 9/18/14.
 */
case class Login(userID: Int)
object Login {
  def getLoggedInUser(userID: Int): Userstate = {
    val endResult = Userstates.getUserByUserID(userID).map {
      case Some(result) => result
      case None => null
    }
    try {
      Await.result(endResult, 500 millis)
    } catch {
      case te: TimeoutException => null
    }
  }

  /*def login = {

  }*/

  def logoutUser(userID: Int) = {
    Await.result(Userstates.deleteUser(userID), 500 millis)
  }

}
