import play.Project._

name := "Site"

version := "1.0"

playScalaSettings

val phantomVersion = "1.2.2"

libraryDependencies ++= Seq(
  "com.websudos"  %% "phantom-dsl"                   % phantomVersion,
  "com.websudos"  %% "phantom-example"               % phantomVersion,
  "com.websudos"  %% "phantom-spark"                 % phantomVersion,
  //"com.websudos"  %% "phantom-test"                  % phantomVersion,
  //"com.websudos"  %% "phantom-testing"               % phantomVersion,
  "com.websudos"  %% "phantom-udt"                   % phantomVersion,
  "com.github.sstone" %% "amqp-client" % "1.4",
  "org.fusesource.mqtt-client" % "mqtt-client" % "1.5",
  //"org.scala-lang" %% "scala-pickling" % "0.8.0",
  "org.apache.spark" %% "spark-core" % "1.1.0",
  "org.apache.spark" %% "spark-sql" % "1.1.0",
  "org.apache.spark" %% "spark-streaming" % "1.1.0",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.1.0-alpha3" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3"
)


//libraryDependencies += "ws.securesocial" %% "securesocial" % "2.1.4"