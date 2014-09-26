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
  "com.websudos"  %% "phantom-udt"                   % phantomVersion
)