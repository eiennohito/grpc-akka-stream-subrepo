
lazy val defaults = Def.settings(
  organization := "org.eiennohito",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8")
)

lazy val coreDeps = Def.settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.4.8",
    "io.grpc" % "grpc-core" % grpcVersion,
    "io.grpc" % "grpc-stub" % grpcVersion,
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  )
)

lazy val `grpc-streaming` =
  (project in file("."))
  .settings(defaults, coreDeps)

lazy val `grpc-tests` =
  (project in file("tests"))
  .settings(defaults)
  .settings(
    name := "grpc-akka-tests",
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % grpcVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.slf4j" % "jul-to-slf4j" % "1.7.21",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.6" % Test,
      "org.scalatest" %% "scalatest" % "2.2.6" % Test,
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
    )
  )
  .dependsOn(`grpc-streaming`)


import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

lazy val scalaPbVersion = "0.5.32"
lazy val grpcVersion = "0.14.1"

def pbScala(): Seq[Setting[_]] = {
  val config = PB.protobufSettings ++ Seq(
    PB.flatPackage in PB.protobufConfig := true,
    PB.javaConversions in PB.protobufConfig := true,
    PB.scalapbVersion := scalaPbVersion,
    PB.runProtoc in PB.protobufConfig := (args =>
      com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))
  )

  val runtimeDep =
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % scalaPbVersion % PB.protobufConfig

  config ++ Seq(
    runtimeDep
  )
}
