import Dependencies.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport._

val idePackagePrefix = settingKey[Option[String]]("IDE package prefix")

ThisBuild / scalaVersion := "3.7.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
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
    libraryDependencies ++= Dependencies.core,
    commonSettings
  )

lazy val apps = (project in file("apps"))
  .dependsOn(model)
  .settings(
    name := "apps",
    libraryDependencies ++= Dependencies.tests.map(_ % Test) ++ Dependencies.apps,
    commonSettings,
    assembly / mainClass := Some("com.crib.bills.dom6maps.apps.HelloApp"),
    Compile / mainClass := Some("com.crib.bills.dom6maps.apps.HelloApp"),
    assembly / assemblyJarName := "apps-assembly.jar"
  )

lazy val root = (project in file("."))
  .aggregate(model, apps)
  .settings(
    name := "dumb-onion-hax"
  )
