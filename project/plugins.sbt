val scalaPbVersion = "0.5.47"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.3")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % scalaPbVersion

