package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp, Resource, ExitCode, Async}
import ch.linkyard.mcp.jsonrpc2.transport.StdioJsonRpcConnection
import ch.linkyard.mcp.server.*
import ch.linkyard.mcp.server.ToolFunction.Effect
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.generic.auto.given
import com.melvinlow.json.schema.generic.auto.given

import model.map.*
import model.map.Renderer.*
import model.map.{MapUploadRequest, MapUploadConfig}
import apps.services.map.{Impl as UploadServiceImpl}

/**
 * MCP server that accepts map uploads, applies configuration, and
 * streams the resulting map back to the client.
 */
object McpMapServer extends IOApp:

  given Codec[MapWidth] =
    Codec.from(Decoder[Int].map(MapWidth.apply), Encoder[Int].contramap(_.value))
  given Codec[MapHeight] =
    Codec.from(Decoder[Int].map(MapHeight.apply), Encoder[Int].contramap(_.value))
  given Codec[MapSize] =
    Codec.forProduct2("x", "y")(MapSize.apply)(ms => (ms.width, ms.height))
  given Codec[MapUploadConfig] =
    Codec.forProduct1("map-size")(MapUploadConfig.apply)(_.mapSize)
  given com.melvinlow.json.schema.JsonSchemaEncoder[MapUploadConfig] =
    new com.melvinlow.json.schema.JsonSchemaEncoder[MapUploadConfig]:
      def schema: io.circe.Json = io.circe.Json.obj()
  given Codec[MapUploadRequest] =
    Codec.forProduct2("config", "map")(MapUploadRequest.apply)(r => (r.config, r.map))

  private object uploadService extends UploadServiceImpl[IO]:
    protected given sequencer: Async[IO] = IO.asyncForIO

  private def processMap(request: MapUploadRequest): IO[String] =
    uploadService.processUpload(request)

  private def uploadTool: ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info(
      "upload-map",
      Some("Upload Map"),
      Some("Uploads a map file and applies configuration"),
      Effect.Destructive(false),
      isOpenWorld = false
    ),
    (input: MapUploadRequest, _) => processMap(input)
  )

  private class Session extends McpServer.Session[IO] with McpServer.ToolProvider[IO]:
    override val serverInfo: ch.linkyard.mcp.protocol.Initialize.PartyInfo = ch.linkyard.mcp.protocol.Initialize.PartyInfo(
      "DOM6 Maps MCP",
      "0.1.0"
    )
    override def instructions: IO[Option[String]] = IO.pure(None)
    override val tools: IO[List[ToolFunction[IO]]] = IO.pure(List(uploadTool))

  private class Server extends McpServer[IO]:
    override def initialize(client: McpServer.Client[IO]): Resource[IO, McpServer.Session[IO]] =
      Resource.pure(Session())

  override def run(args: List[String]): IO[ExitCode] =
    Server().start(
      StdioJsonRpcConnection.resource[IO],
      e => IO.println(s"Error: $e")
    ).useForever.as(ExitCode.Success)

