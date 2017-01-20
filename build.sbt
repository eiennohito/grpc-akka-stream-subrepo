
lazy val defaults = Def.settings(
  organization := "org.eiennohito",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8")
)

lazy val coreDeps = Def.settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.4.11",
    "io.grpc" % "grpc-core" % grpcVersion,
    "io.grpc" % "grpc-stub" % grpcVersion,
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  )
)

lazy val publishing = Def.settings(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val `grpc-streaming` =
  (project in file("."))
  .settings(defaults, coreDeps, publishing)

lazy val `grpc-tests` =
  (project in file("tests"))
  .settings(defaults, pbScala())
  .settings(
    name := "grpc-akka-tests",
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % grpcVersion,
      "org.slf4j" % "jul-to-slf4j" % "1.7.21",
      "ch.qos.logback" % "logback-classic" % "1.1.7" % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.11" % Test,
      "org.scalatest" %% "scalatest" % "3.0.0" % Test,
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test
    )
  )
  .dependsOn(`grpc-streaming`)


lazy val scalaPbVersion = "0.5.43"
lazy val grpcVersion = "1.0.1"

def pbScala(): Seq[Setting[_]] = {
  Def.settings(
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = true, javaConversions = true, grpc = true) -> (sourceManaged in Compile).value,
      PB.gens.java -> (sourceManaged in Compile).value
    ),
    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.43" % "protobuf"
    )
  )
}
