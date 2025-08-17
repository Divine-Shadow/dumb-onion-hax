package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapDirective, MapState, Renderer, MapDirectiveCodecs}
import model.map.Renderer.*
import java.nio.charset.StandardCharsets

trait MapWriter[Sequencer[_]]:
  def write[ErrorChannel[_]](
      state: MapState,
      passThrough: Vector[MapDirective],
      output: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]]

  def write[ErrorChannel[_]](
      state: MapState,
      output: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    write(state, Vector.empty, output)

class MapWriterImpl[Sequencer[_]: Async: Files] extends MapWriter[Sequencer]:
  protected val sequencer = summon[Async[Sequencer]]

  override def write[ErrorChannel[_]](
      state: MapState,
      passThrough: Vector[MapDirective],
      output: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Unit]] =
    val directives = MapDirectiveCodecs.merge(state, passThrough)
    val bytes = directives.map(_.render).mkString("\n").getBytes(StandardCharsets.UTF_8)
    for
      _ <- sequencer.delay(println(s"Writing map to $output"))
      _ <- Files[Sequencer].createDirectories(output.parent.getOrElse(output))
      _ <- Files[Sequencer]
        .writeAll(output)
        .apply(Stream.emits(bytes).covary[Sequencer])
        .compile
        .drain
    yield ().pure[ErrorChannel]
