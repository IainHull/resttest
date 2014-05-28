import Dependencies._

name := "resttest"

version := "1.0"

scalaVersion := "2.11.1"

resolvers += "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io/"

libraryDependencies ++= resttestDependencies

parallelExecution in Test := false

net.virtualvoid.sbt.graph.Plugin.graphSettings


