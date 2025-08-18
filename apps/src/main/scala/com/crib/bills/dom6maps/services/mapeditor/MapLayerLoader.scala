package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapDirective, MapFileParser, MapState, MapLayer}

trait MapLayerLoader[Sequencer[_]]:
    def load[ErrorChannel[_]](
        path: Path
    )(using
        files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[MapLayer[Sequencer]]]

    def load[ErrorChannel[_]](
        bytes: Stream[Sequencer, Byte]
    )(using
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[MapLayer[Sequencer]]]

class MapLayerLoaderImpl[Sequencer[_]: Async] extends MapLayerLoader[Sequencer]:
    override def load[ErrorChannel[_]](
        path: Path
    )(using
        files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[MapLayer[Sequencer]]] =
      MapState
        .fromDirectivesWithPassThrough(MapFileParser.parseFile[Sequencer](path))
        .attempt
        .map {
          case Left(e)       => errorChannel.raiseError[MapLayer[Sequencer]](e)
          case Right(parsed) => errorChannel.pure(parsed)
        }

    override def load[ErrorChannel[_]](
        bytes: Stream[Sequencer, Byte]
    )(using
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[MapLayer[Sequencer]]] =
      MapState
        .fromDirectivesWithPassThrough(bytes.through(MapFileParser.parse[Sequencer]))
        .attempt
        .map {
          case Left(e)       => errorChannel.raiseError[MapLayer[Sequencer]](e)
          case Right(parsed) => errorChannel.pure(parsed)
        }

class MapLayerLoaderStub[Sequencer[_]: Applicative](state: MapState, passThrough: Vector[MapDirective]) extends MapLayerLoader[Sequencer]:
  override def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapLayer[Sequencer]]] =
    MapLayer(state, fs2.Stream.emits(passThrough).covary[Sequencer]).pure[Sequencer].map(_.pure[ErrorChannel])

  override def load[ErrorChannel[_]](
      bytes: Stream[Sequencer, Byte]
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapLayer[Sequencer]]] =
    MapLayer(state, fs2.Stream.emits(passThrough).covary[Sequencer]).pure[Sequencer].map(_.pure[ErrorChannel])
