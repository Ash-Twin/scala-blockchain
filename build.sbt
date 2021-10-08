name := "scala-blockchain"

version := "0.1"

scalaVersion := "2.13.6"
val AkkaVersion       = "2.6.16"
val AkkaHttpVersion   = "10.2.6"
val circeVersion      = "0.14.1"
val pureConfigVersion = "0.16.0"
val SlickVersion      = "3.3.3"
libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor-typed"           % AkkaVersion,
  "com.typesafe.akka"  %% "akka-http"                  % AkkaHttpVersion,
  "com.typesafe.akka"  %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka"  %% "akka-persistence-typed"     % AkkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc"      % "5.0.4",
  "com.typesafe.akka"  %% "akka-persistence-query"     % AkkaVersion,
  "com.typesafe.akka"  %% "akka-actor-testkit-typed"   % AkkaVersion % Test,
  "com.typesafe.akka"  %% "akka-persistence-testkit"   % AkkaVersion % Test
)
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"          % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion
)
libraryDependencies ++= Seq("com.github.pureconfig" %% "pureconfig", "com.github.pureconfig" %% "pureconfig-akka")
  .map(_ % pureConfigVersion)

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.24"

libraryDependencies += "org.slf4j"       % "slf4j-api"   % "2.0.0-alpha5"
libraryDependencies += "net.codingwell" %% "scala-guice" % "5.0.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
resolvers += Resolver.sonatypeRepo("snapshots")
