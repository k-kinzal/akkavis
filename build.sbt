name := "akkavis"
organization := "kinzal"
version := "0.1.0"

scalaVersion := "2.12.10"

Compile / mainClass := Some("sample.ClusterRunner")

val akkaVersion = "2.6.5"
lazy val akkaHttpVersion = "10.1.11"
lazy val akkaMgmtVersion = "1.0.7"

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin, PackPlugin)
  .configs(MultiJvm)
  .settings(
    scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature"),
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnEvictionSummary(false),

    packMain := Map("akkavis" -> "sample.ClusterRunner"),

    libraryDependencies ++= Seq(
      "net.liftweb" %% "lift-json" % "3.3.0",

      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,

      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,

      "com.lightbend.akka.management" %% "akka-management" % akkaMgmtVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaMgmtVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaMgmtVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaMgmtVersion,

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

      "org.scalatest" %% "scalatest" % "3.0.7" % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,

      "io.kamon" %% "kamon-bundle" % "2.0.0",
      "io.kamon" %% "kamon-akka" % "2.1.0",
      "io.kamon" %% "kamon-akka-http" % "2.1.0",
      "io.kamon" %% "kamon-prometheus" % "2.1.0",
      "io.kamon" %% "kamon-zipkin" % "1.0.0",
      "io.kamon" %% "kamon-logback" % "2.1.0",
      "io.kamon" %% "kamon-system-metrics" % "2.1.0"
    )
  )

