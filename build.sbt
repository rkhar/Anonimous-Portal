name := "anon-portal"

version := "0.0.1"

scalaVersion := "2.13.0"

val elastic4sVersion      = "7.3.1"
val akkaVersion           = "2.6.1"
val akkaHttpVersion       = "10.1.11"
val json4sVersion         = "3.6.7"
val logbackClassicVersion = "1.2.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-http"               % akkaHttpVersion,
  "de.heikoseeberger"      %% "akka-http-json4s"        % "1.30.0",
  "com.typesafe.akka"      %% "akka-stream"             % "2.5.23",
  "com.typesafe.akka"      %% "akka-actor-typed"        % akkaVersion,
  "com.typesafe.akka"      %% "akka-slf4j"              % akkaVersion,
  "org.json4s"             %% "json4s-core"             % json4sVersion,
  "org.json4s"             %% "json4s-jackson"          % json4sVersion,
  "org.json4s"             %% "json4s-native"           % json4sVersion,
  "ch.qos.logback"         % "logback-classic"          % logbackClassicVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-core"          % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams"  % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-testkit"       % elastic4sVersion % "test",
  "com.outr"               %% "hasher"                  % "1.2.2"
)
