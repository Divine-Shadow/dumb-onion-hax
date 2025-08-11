package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async
import model.map.{MapFileParser, MapState}

trait MapLayerLoader[Sequencer[_]]:
  def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]]

class MapLayerLoaderImpl[Sequencer[_]: Async] extends MapLayerLoader[Sequencer]:
  override def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    MapState
      .fromDirectives(MapFileParser.parseFile[Sequencer](path))
      .attempt
      .map {
        case Left(e)  => errorChannel.raiseError[MapState](e)
        case Right(ms) => errorChannel.pure(ms)
      }

class MapLayerLoaderStub[Sequencer[_]: Applicative](state: MapState) extends MapLayerLoader[Sequencer]:
  override def load[ErrorChannel[_]](
      path: Path
  )(using
      files: Files[Sequencer],
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    state.pure[Sequencer].map(_.pure[ErrorChannel])
