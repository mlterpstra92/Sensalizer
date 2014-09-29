package models

import scala.concurrent._
import scala.concurrent.duration._
import com.websudos.phantom.Implicits._

/**
 * Created by maarten on 9/22/14.
 */
case class Authorization(userID: Long)
object Authorization{
  def isAuthorized(userID: Int): Boolean = {
    val endResult = Userstates.getUserByUserID(userID).map {
      case Some(result) => result.userID == userID
      case None => false
    }
    Await.result(endResult, 500 millis)
  }
}
