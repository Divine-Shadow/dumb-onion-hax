package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp, Resource}
import cats.effect.ExitCode
import cats.implicits.*
import ch.linkyard.mcp.jsonrpc2.transport.StdioJsonRpcConnection
import ch.linkyard.mcp.protocol.Initialize.PartyInfo
import ch.linkyard.mcp.server.*
import ch.linkyard.mcp.server.ToolFunction.Effect
import com.melvinlow.json.schema.generic.auto.given
import io.circe.generic.auto.given

/**
 * Minimal MCP server showcasing scala-effect-mcp integration.
 * Provides a single "echo" tool that returns the supplied text.
 */
object McpEchoServer extends IOApp:
  case class EchoInput(text: String)

  private def echoTool: ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info(
      "echo",
      Some("Echo"),
      Some("Repeats the input text back to you"),
      Effect.ReadOnly,
      isOpenWorld = false
    ),
    (input: EchoInput, _) => IO.pure(input.text)
  )

  private class Session extends McpServer.Session[IO] with McpServer.ToolProvider[IO]:
    override val serverInfo: PartyInfo = PartyInfo(
      "DOM6 Maps Echo MCP",
      "0.1.0"
    )
    override def instructions: IO[Option[String]] = IO.pure(None)
    override val tools: IO[List[ToolFunction[IO]]] = IO.pure(List(echoTool))

  private class Server extends McpServer[IO]:
    override def initialize(client: McpServer.Client[IO]): Resource[IO, McpServer.Session[IO]] =
      Resource.pure(Session())

  override def run(args: List[String]): IO[ExitCode] =
    Server().start(
      StdioJsonRpcConnection.resource[IO],
      e => IO.println(s"Error: $e")
    ).useForever.as(ExitCode.Success)

