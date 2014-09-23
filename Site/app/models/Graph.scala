package models

import scalax.chart.api._

/**
 * Created by maarten on 9/23/14.
 */
case class Graph (feed: Feed)

object Graph {
    def plot(feed: Feed) = {
        val data = for (i <- 1 to 5) yield (i,i)
        val chart = XYLineChart(data)
        chart
    }
}