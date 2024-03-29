/*
 * Copyright 2013 websudos ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models

import com.datastax.driver.core.{Cluster, Session}
import com.websudos.phantom.Implicits._

import scala.concurrent.{Future, blocking}

object CassandraConnector {
  val keySpace = "sensalizer"

  lazy val cluster = Cluster.builder()
    .addContactPoints("54.77.184.240","54.171.11.163","54.171.159.183")
    .withPort(9042)
    .withoutJMXReporting()
    .withoutMetrics()
    .build()

  println("Connected to cluster ", cluster.getMetadata.getClusterName)
  println("Hosts: ", cluster.getMetadata.getAllHosts)
  lazy val session = blocking {
    cluster.connect(keySpace)
  }
}

trait CassandraConnector {
  self: CassandraTable[_, _] =>

  def createTable(): Future[Unit] ={
    create.future() map (_ => ())
  }

  implicit lazy val datastax: Session = CassandraConnector.session
}