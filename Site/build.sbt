name := "Site"

scalaVersion := "2.10.4"

version := "1.0"

playScalaSettings

val phantomVersion = "1.2.2"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.1.0",
  "org.apache.spark" %% "spark-streaming" % "1.1.0",
  "org.apache.spark" %% "spark-sql" % "1.1.0",
  "org.apache.spark" %% "spark-hive" % "1.1.0",
  "org.apache.spark" %% "spark-mllib" % "1.1.0",
  "com.websudos"  %% "phantom-dsl"                   % phantomVersion,
  "com.typesafe.akka" %% "akka-actor" % "2.2.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.4",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.1.0-beta1" withSources() withJavadoc(),
  "com.github.sstone" %% "amqp-client" % "1.4",
  "org.fusesource.mqtt-client" % "mqtt-client" % "1.5"
)