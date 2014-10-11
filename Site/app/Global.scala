/**
 * Created by folkert on 23-9-14.
 */

import models.Datastream
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.collection.mutable
import scala.concurrent.Future

object Global extends GlobalSettings {
  val queue: mutable.PriorityQueue[Datastream] = new mutable.PriorityQueue[Datastream]();

  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {
    Future.successful(InternalServerError(views.html.errors.onError(throwable)))
  }

  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader)= {
    Future.successful(NotFound(views.html.errors.onHandlerNotFound(request)))
  }

}