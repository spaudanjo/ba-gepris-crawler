
name := "gepriscrawler"

version := "0.3"

scalaVersion := "2.12.6"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-actor" % "2.5.14",
  "com.typesafe.akka" %% "akka-stream" % "2.5.14",
  "org.jsoup" % "jsoup" % "1.11.2",
  "ch.qos.logback" % "logback-classic" % "1.2.+",
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",
  "org.specs2" %% "specs2-core" % "4.2.0",
  "com.typesafe" % "config" % "1.3.2",
  "commons-io" % "commons-io" % "2.6",
  "org.zeroturnaround" % "zt-zip" % "1.13",

  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "org.scalikejdbc" %% "scalikejdbc" % "3.1.+",
  "org.scalikejdbc" %% "scalikejdbc-streams" % "3.1.0",
  "com.github.scopt" %% "scopt" % "3.7.0"

)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
mainClass in Compile := Some("gepriscrawler.App")

scalacOptions in Test ++= Seq("-Yrangepos")