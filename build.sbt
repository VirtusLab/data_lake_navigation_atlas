
name               := "atlas-with-spark"
scalaVersion       := "2.11.11"
organization       := "com.virtuslab"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

lazy val buildSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka"  %% "akka-http"        % "10.0.7",
    "de.heikoseeberger"  %% "akka-http-json4s" % "1.16.0",
    "org.json4s"         %% "json4s-native"    % "3.5.2",
    "org.json4s"         %% "json4s-ext"       % "3.5.2",
    "org.apache.spark"   %% "spark-core"       % "2.1.0",
    "org.scalatest"      %% "scalatest"        % "3.0.0" % "test"
  )
)

lazy val atlasWithSpark = project.in(file("."))
  .settings(buildSettings)

