import sbt._

object Dependencies {
  val jerseyVersion = "1.17.+"
  val scalatestVersion = "2.1.6"
  val sprayVersion = "1.3.1-20140423"
  val playVersion = "2.3.0-RC2"

  val jersey = "com.sun.jersey" % "jersey-core" % jerseyVersion
  val jerseyClient = "com.sun.jersey" % "jersey-client" % jerseyVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val playJson = "com.typesafe.play" %% "play-json" % playVersion

  val sprayCan = "io.spray" %% "spray-can" % sprayVersion
  val sprayRouting = "io.spray" %% "spray-routing" % sprayVersion
  val sprayTestkit = "io.spray" %% "spray-testkit" % sprayVersion

  val resttestDependencies = Seq(jersey, jerseyClient, scalatest, playJson, sprayCan, sprayRouting, sprayTestkit)
}
