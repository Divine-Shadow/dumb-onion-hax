import sbt._

object Dependencies {
  private val scalacheckVersion = "1.17.0"
  private val catsVersion       = "2.10.0"
  private val fs2Version        = "3.10.2"
  private val log4catsVersion   = "2.7.0"
  private val fastparseVersion  = "3.1.1"
  private val weaverVersion     = "0.8.3"
  private val mcpVersion        = "0.2.0"

  lazy val core = Seq(
    "org.typelevel" %% "cats-core"     % catsVersion,
    "co.fs2"       %% "fs2-core"      % fs2Version,
    "co.fs2"       %% "fs2-io"        % fs2Version,
    "org.typelevel" %% "log4cats-core" % log4catsVersion,
    "com.lihaoyi"  %% "fastparse"     % fastparseVersion
  )

  lazy val tests = Seq(
    "org.scalacheck" %% "scalacheck"     % scalacheckVersion,
    "com.disneystreaming" %% "weaver-cats"      % weaverVersion,
    "com.disneystreaming" %% "weaver-scalacheck" % weaverVersion
  )

  lazy val apps = Seq(
    "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
    "com.lihaoyi" %% "fastparse"          % "3.1.0",
    "ch.linkyard.mcp" %% "mcp-server"     % mcpVersion,
    "ch.linkyard.mcp" %% "jsonrpc2-stdio" % mcpVersion,
  )
}
