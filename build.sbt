name := "grpc-akka-stream"

version := "0.1-SNAPSHOT"

organization := "org.eiennohito"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.6",
  "io.grpc" % "grpc-core" % "0.14.0",
  "io.grpc" % "grpc-stub" % "0.14.0"
)

net.virtualvoid.sbt.graph.Plugin.graphSettings
