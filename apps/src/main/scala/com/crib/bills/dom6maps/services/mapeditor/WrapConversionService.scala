package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import cats.effect.Sync
import model.map.MapState

trait WrapConversionService[Sequencer[_]]:
  def convert[ErrorChannel[_]](
      state: MapState,
      target: WrapChoice
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]]

class WrapConversionServiceImpl[Sequencer[_]: Sync] extends WrapConversionService[Sequencer]:
  protected val sequencer = summon[Sync[Sequencer]]

  override def convert[ErrorChannel[_]](
      state: MapState,
      target: WrapChoice
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[MapState]] =
    val result = target match
      case WrapChoice.HWrap => WrapSeverService.severVertically(state)
      case WrapChoice.VWrap => WrapSeverService.severHorizontally(state)
      case WrapChoice.NoWrap =>
        WrapSeverService.severHorizontally(WrapSeverService.severVertically(state))
      case WrapChoice.GroundSurfaceDuel => state
    sequencer.delay(println(s"Converting map to $target")).as(errorChannel.pure(result))
