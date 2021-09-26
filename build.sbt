name := "scala-blockchain"

version := "0.1"

scalaVersion := "2.13.6"
val AkkaVersion = "2.6.16"
val circeVersion = "0.14.1"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test
)
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.16.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)