package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse, Applicative}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapDirective, MapFileParser}

trait MapLayerLoader[Sequencer[_]]:
  def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]]

class MapLayerLoaderImpl[Sequencer[_]: Async] extends MapLayerLoader[Sequencer]:
  override def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]] =
    MapFileParser
      .parseFile[Sequencer](path)
      .compile
      .toVector
      .attempt
      .map {
        case Left(e)  => errorChannel.raiseError[Vector[MapDirective]](e)
        case Right(ds) => errorChannel.pure(ds)
      }

class MapLayerLoaderStub[Sequencer[_]: Applicative](directives: Vector[MapDirective]) extends MapLayerLoader[Sequencer]:
  override def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]] =
    directives.pure[Sequencer].map(_.pure[ErrorChannel])
