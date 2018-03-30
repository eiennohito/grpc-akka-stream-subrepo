import sbt.internal.LoadedBuild

lazy val defaults = Def.settings(
  organization := "org.eiennohito",
  version := "0.1-SNAPSHOT",
  (scalaVersion in ThisBuild) := (if (isRoot(loadedBuild.value, thisProject.value)) "2.12.4" else scalaVersion.value),
  crossScalaVersions := Seq("2.11.12", "2.12.4")
)

lazy val coreDeps = Def.settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.5.9",
    "io.grpc" % "grpc-core" % grpcVersion,
    "io.grpc" % "grpc-stub" % grpcVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  )
)

def isRoot(bld: LoadedBuild, proj: ResolvedProject): Boolean = {
  val refUri = proj.base.toURI
  val rootUri = bld.root
  val areEqual = rootUri.getScheme == "file" && rootUri == refUri
  areEqual
}

lazy val `grpc-streaming` =
  (project in file("."))
  .settings(defaults, coreDeps)

lazy val `grpc-tests` =
  (project in file("tests"))
  .settings(defaults, pbScala())
  .settings(
    name := "grpc-akka-tests",
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % grpcVersion,
      "org.slf4j" % "jul-to-slf4j" % "1.7.22",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.9" % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test
    )
  )
  .dependsOn(`grpc-streaming`)


lazy val scalaPbVersion = "0.7.0"
lazy val grpcVersion = "1.10.0"

def pbScala(): Seq[Setting[_]] = {
  Def.settings(
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true, flatPackage = true) -> (sourceManaged in Compile).value
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalaPbVersion % "protobuf"
    )
  )
}
