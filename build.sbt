name := "grpc-akka-stream"

version := "0.1-SNAPSHOT"

def checkLibrary(org: String, name: String): ModuleID => Boolean = {
  x => x.organization == org && x.name == name
}

libraryDependencies := {
  val present = libraryDependencies.value
  var added: List[ModuleID] = Nil
  if (!present.exists(checkLibrary("io.grpc", "grpc-core" ))) {
    added = ("io.grpc" % "grpc-core" % "0.13.2") :: added
  }

  if (!present.exists(checkLibrary("com.typesafe.akka", "akka-stream"))) {
    added = ("com.typesafe.akka" %% "akka-stream" % "2.4.4") :: added
  }

  present ++ added
}
