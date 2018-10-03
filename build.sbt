import sbtassembly.MergeStrategy

name := "akka-http-microservice"

version := "0.1"

scalaVersion := "2.12.7"

val akkaVersion = "2.5.17"
val akkaHttpVersion = "10.1.5"
val cassandraDriverVersion = "3.6.0"
val cassandraUnitVersion = "3.5.0.1"
val scalaTestVersion = "3.0.5"
val mockitoVersion = "1.10.19"
val swaggerVersion = "1.0.0"
val loggingVersion = "3.9.0"
val jwtVersion = "0.18.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.datastax.cassandra" % "cassandra-driver-core" % cassandraDriverVersion,
  "org.cassandraunit" % "cassandra-unit" % cassandraUnitVersion,
  "com.github.swagger-akka-http" %% "swagger-akka-http" % swaggerVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % loggingVersion,
  "com.pauldijou" %% "jwt-core" % jwtVersion,
  
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.cassandraunit" % "cassandra-unit" % cassandraUnitVersion % Test,
  "org.mockito" % "mockito-all" % mockitoVersion % Test
)

enablePlugins(DockerPlugin, JavaAppPackaging)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties", xs@_*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("io", "netty", xs@_*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "logback.xml" => MergeStrategy.first
  case "application.conf" => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case PathList("org", "apache", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("akka", "stream", xs@_*) => MergeStrategy.first
  case x => (assemblyMergeStrategy in assembly).value(x)
}
