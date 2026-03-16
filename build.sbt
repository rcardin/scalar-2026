ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "scalar-2026",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % "2.0.16"
    )
  )
