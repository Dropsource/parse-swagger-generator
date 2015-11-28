name := "parse-swagger-generator"

organization := "dropsource"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.0.0",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.play" %% "play-json" % "2.4.4",
  "io.swagger" % "swagger-parser" % "1.0.13"
)
    