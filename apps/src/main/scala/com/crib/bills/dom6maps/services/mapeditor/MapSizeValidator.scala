package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{Applicative, MonadError, Traverse}
import cats.syntax.all.*
import model.map.*

trait MapSizeValidator[Sequencer[_]]:
  def validate[ErrorChannel[_]](
      surface: MapState,
      cave: MapState
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, MapState, MapState)]]

class MapSizeValidatorImpl[Sequencer[_]: Applicative] extends MapSizeValidator[Sequencer]:
  override def validate[ErrorChannel[_]](
      surface: MapState,
      cave: MapState
    )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, MapState, MapState)]] =
    val result = (surface.size, cave.size) match
      case (Some(surf), Some(caveSize)) =>
        if surf.value != caveSize.value then
          Left(IllegalArgumentException("Map dimensions must be equal, square, and odd"))
        else Right((surf, surface, cave))
      case _ => Left(IllegalArgumentException("Map size directive missing"))
    val ec = result match
      case Left(e)      => errorChannel.raiseError[(MapSize, MapState, MapState)](e)
      case Right(value) => errorChannel.pure(value)
    ec.pure[Sequencer]

class MapSizeValidatorStub[Sequencer[_]: Applicative](
    size: MapSize,
    surfaceState: MapState,
    caveState: MapState
) extends MapSizeValidator[Sequencer]:
  override def validate[ErrorChannel[_]](
      surface: MapState,
      cave: MapState
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ): Sequencer[ErrorChannel[(MapSize, MapState, MapState)]] =
    (size, surfaceState, caveState).pure[ErrorChannel].pure[Sequencer]
