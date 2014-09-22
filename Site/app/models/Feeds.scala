package models

import scala.collection.mutable.ArrayBuffer

/**
 * Created by maarten on 9/18/14.
 */
case class Feed(feedID: Int, title: String, data: List[String])
object Feed {

}

case class Feeds(feedID: Int)
object Feeds {

  var feeds : ArrayBuffer[Feed] = ArrayBuffer()
  def getList: List[Feed] = {

    feeds += Feed(1, "hurrdurr", ArrayBuffer("This: that").toList)
    feeds.toList
  }

  def getFeed(a_feedID: Int): Feed = {
    assert(a_feedID > 0 && a_feedID < feeds.length)
    feeds.apply(a_feedID)
  }
}
