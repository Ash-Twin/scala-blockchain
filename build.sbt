name := "scala-blockchain"

version := "0.1"

scalaVersion := "2.13.6"
val AkkaVersion       = "2.6.16"
val circeVersion      = "0.14.1"
val pureConfigVersion = "0.16.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"         % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed"   % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test
)
libraryDependencies ++= Seq("com.github.pureconfig" %% "pureconfig", "com.github.pureconfig" %% "pureconfig-akka")
  .map(_ % pureConfigVersion)

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.0-alpha5"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
