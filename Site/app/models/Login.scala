package models

/**
 * Created by maarten on 9/18/14.
 */
case class Login(userID: Long)
object Login {
  def loggedIn(userID: Long): Boolean = {
    true
  }

  def getLoggedInUser(userID: Long): String = {
    if (userID == 0) {
      "Maarten"
    }
    else null
  }
}
