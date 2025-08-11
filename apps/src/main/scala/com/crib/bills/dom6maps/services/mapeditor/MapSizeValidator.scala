package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse, Applicative}
import cats.syntax.all.*
import fs2.Stream
import model.map.*

trait MapSizeValidator[Sequencer[_]]:
  def validate[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective]
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, Vector[MapDirective], Vector[MapDirective])]]

class MapSizeValidatorImpl[Sequencer[_]: cats.effect.Sync] extends MapSizeValidator[Sequencer]:
  override def validate[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective]
    )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, Vector[MapDirective], Vector[MapDirective])]] =
    for
      surfaceDirectives <- surface.compile.toVector
      caveDirectives    <- cave.compile.toVector
    yield
      val surfSize = surfaceDirectives.collectFirst { case m: MapSizePixels => m.toProvinceSize }
      val caveSize = caveDirectives.collectFirst { case m: MapSizePixels => m.toProvinceSize }
      val result = (surfSize, caveSize) match
        case (
              Some(ProvinceSize(MapWidth(sw), MapHeight(sh))),
              Some(ProvinceSize(MapWidth(cw), MapHeight(ch)))
            ) =>
          if sw != sh || cw != ch || sw != cw then
            Left(IllegalArgumentException("Map dimensions must be equal, square, and odd"))
          else MapSize.from(sw).map(ms => (ms, surfaceDirectives, caveDirectives))
        case _ => Left(IllegalArgumentException("Map size directive missing"))
      errorChannel.fromEither(result)

class MapSizeValidatorStub[Sequencer[_]: Applicative](
    size: MapSize,
    surfaceDirectives: Vector[MapDirective],
    caveDirectives: Vector[MapDirective]
) extends MapSizeValidator[Sequencer]:
  override def validate[ErrorChannel[_]](
      surface: Stream[Sequencer, MapDirective],
      cave: Stream[Sequencer, MapDirective]
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, Vector[MapDirective], Vector[MapDirective])]] =
    (size, surfaceDirectives, caveDirectives).pure[ErrorChannel].pure[Sequencer]
