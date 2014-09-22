package models

/**
 * Created by maarten on 9/22/14.
 */
case class Authorization(userID: Long)
object Authorization {
  def isAuthorized(userID: Long) : Boolean = {
    true
  }
}
