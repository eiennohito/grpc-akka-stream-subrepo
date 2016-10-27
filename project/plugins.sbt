val scalaPbVersion = "0.5.43"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.1")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % scalaPbVersion

