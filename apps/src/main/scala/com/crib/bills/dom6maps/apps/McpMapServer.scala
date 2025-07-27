package com.crib.bills.dom6maps
package apps

import cats.effect.{IO, IOApp, Resource}
import cats.effect.ExitCode
import cats.implicits.*
import ch.linkyard.mcp.jsonrpc2.transport.StdioJsonRpcConnection
import ch.linkyard.mcp.server.*
import ch.linkyard.mcp.server.ToolFunction.Effect
import fs2.Stream
import java.nio.charset.StandardCharsets
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.generic.auto.given
import com.melvinlow.json.schema.generic.auto.given
import io.circe.syntax.*

import model.map.*
import model.map.Renderer.*

/**
 * MCP server that accepts map uploads, applies configuration, and
 * streams the resulting map back to the client.
 */
object McpMapServer extends IOApp:

  final case class MapUploadConfig(mapSize: MapSize)
  object MapUploadConfig:
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

  import MapUploadConfig.given

  final case class UploadRequest(config: MapUploadConfig, map: String)

  private def processMap(request: UploadRequest): IO[String] =
    val mapBytes = request.map.getBytes(StandardCharsets.UTF_8)
    MapFileParser
      .parse[IO]
      .apply(Stream.emits(mapBytes).covary[IO])
      .compile
      .toVector
      .map { directives =>
        val rest = directives.filterNot(_.isInstanceOf[MapSize])
        val newDirectives = MapSize(request.config.mapSize.width, request.config.mapSize.height) +: rest
        newDirectives.map(_.render).mkString("\n")
      }

  private def uploadTool: ToolFunction[IO] = ToolFunction.text(
    ToolFunction.Info(
      "upload-map",
      Some("Upload Map"),
      Some("Uploads a map file and applies configuration"),
      Effect.Destructive(false),
      isOpenWorld = false
    ),
    (input: UploadRequest, _) => processMap(input)
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

