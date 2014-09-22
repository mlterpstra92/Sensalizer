package models

import scala.collection.mutable.ArrayBuffer

/**
 * Created by maarten on 9/18/14.
 */
case class Feed(feedID: Long, title: String)
object Feed {

  def getList: List[Feed] = {
    var feeds : ArrayBuffer[Feed] = ArrayBuffer()

    feeds += Feed(1, "hurrdurr")
    feeds.toList
  }
}
