package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.Applicative
import cats.syntax.all.*
import model.map.{MapDirective, MapHeight, MapWidth}

trait WrapConversionService[Sequencer[_]]:
  def convert[ErrorChannel[_]](
      directives: Vector[MapDirective],
      width: MapWidth,
      height: MapHeight,
      target: WrapChoice
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]]

class WrapConversionServiceImpl[Sequencer[_]: Applicative] extends WrapConversionService[Sequencer]:
  override def convert[ErrorChannel[_]](
      directives: Vector[MapDirective],
      width: MapWidth,
      height: MapHeight,
      target: WrapChoice
  )(using
      errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]] =
    val result = target match
      case WrapChoice.HWrap => WrapSeverService.severVertically(directives, width, height)
      case WrapChoice.VWrap => WrapSeverService.severHorizontally(directives, width, height)
      case WrapChoice.NoWrap =>
        val vertical = WrapSeverService.severVertically(directives, width, height)
        WrapSeverService.severHorizontally(vertical, width, height)
      case WrapChoice.GroundSurfaceDuel => directives
    result.pure[Sequencer].map(errorChannel.pure)
