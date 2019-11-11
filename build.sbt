name := "singup-login-project"

version := "0.0.1"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % "10.1.10",
  "com.typesafe.akka" %% "akka-stream"          % "2.5.23",
  "com.typesafe.akka" %% "akka-actor"           % "2.5.23",
  "ch.megard"         %% "akka-http-cors"       % "0.4.1",
  "de.heikoseeberger" %% "akka-http-circe"      % "1.29.1",
  "de.heikoseeberger" %% "akka-http-json4s"     % "1.29.1",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10",
  "org.json4s"        %% "json4s-core"          % "3.6.7",
  "org.json4s"        %% "json4s-jackson"       % "3.6.7",
  "org.json4s"        %% "json4s-native"        % "3.6.7"
)
