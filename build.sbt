import Dependencies.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport._

val idePackagePrefix = settingKey[Option[String]]("IDE package prefix")

ThisBuild / scalaVersion := "3.7.1"
ThisBuild / version := "1.1"
ThisBuild / organization := "com.crib.bills"
ThisBuild / organizationName := "Bill's Crib"
Global / excludeLintKeys := Set(idePackagePrefix)

lazy val commonSettings = Seq(
  idePackagePrefix := Some("com.crib.bills.dom6maps"),
  testFrameworks += new TestFramework("org.scalacheck.ScalaCheckFramework"),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

lazy val model = (project in file("model"))
  .settings(
    name := "model",
    libraryDependencies ++= Dependencies.core ++ Dependencies.tests.map(_ % Test),
    commonSettings
  )

lazy val apps = (project in file("apps"))
  .dependsOn(model)
  .settings(
    name := "apps",
    libraryDependencies ++= Dependencies.tests.map(_ % Test) ++ Dependencies.apps,
    commonSettings,
    // Run the app in a forked JVM so we can pass proper JVM options
    Compile / run / fork := true,
    // Allow JVM options to be provided via env var or system property without
    // confusing sbt's command parser (e.g., APP_JAVA_OPTS="-Xms4G -Xmx4G")
    Compile / run / javaOptions ++= {
      val fromEnv  = sys.env.get("APP_JAVA_OPTS").toSeq.flatMap(_.split("\\s+").filter(_.nonEmpty))
      val fromProp = sys.props.get("app.java.opts").toSeq.flatMap(_.split("\\s+").filter(_.nonEmpty))
      fromEnv ++ fromProp
    },
    assembly / mainClass := Some("com.crib.bills.dom6maps.apps.MapEditorWrapApp"),
    Compile / mainClass := Some("com.crib.bills.dom6maps.apps.MapEditorWrapApp"),
    assembly / assemblyJarName := "dumb-onion-hax.jar"
  )

lazy val root = (project in file("."))
  .aggregate(model, apps)
  .settings(
    name := "dumb-onion-hax"
  )
