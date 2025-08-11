package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapDirective, MapState, Renderer}
import model.map.Renderer.*
import model.map.MapDirectiveCodecs.Encoder
import model.map.MapDirectiveCodecs.Encoder.given
import java.nio.charset.StandardCharsets

trait MapWriter[Sequencer[_]]:
  def write[ErrorChannel[_]](
      state: MapState,
      output: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

class MapWriterImpl[Sequencer[_]: Async: Files] extends MapWriter[Sequencer]:
  override def write[ErrorChannel[_]](
      state: MapState,
      output: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    val directives = Encoder[MapState].encode(state)
    val bytes = directives.map(_.render).mkString("\n").getBytes(StandardCharsets.UTF_8)
    Files[Sequencer]
      .createDirectories(output.parent.getOrElse(output))
      >> Files[Sequencer]
        .writeAll(output)
        .apply(Stream.emits(bytes).covary[Sequencer])
        .compile
        .drain
        .as(().pure[ErrorChannel])
