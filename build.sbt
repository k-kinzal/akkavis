import com.typesafe.sbt.SbtNativePackager.autoImport.packageName
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := "akka-cluster-sharding-visualizer"
organization := "org.lightbend"
version := "0.2"

scalaVersion := "2.12.7"

Compile/mainClass := Some("sample.ClusterRunner")

val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.1.8"
lazy val akkaMgmtVersion   = "1.0.1"

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin, JavaAppPackaging, DockerPlugin)
  .configs(MultiJvm)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,

        "net.liftweb" %% "lift-json" % "3.3.0",

        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
        "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,

        "com.lightbend.akka.management" %% "akka-management"                   % akkaMgmtVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http"      % akkaMgmtVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaMgmtVersion,
        "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
        "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % akkaMgmtVersion,

        "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

        "org.scalatest" %% "scalatest" % "3.0.7" % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test
      )
  )

