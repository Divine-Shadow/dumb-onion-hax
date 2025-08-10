package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.Applicative
import cats.syntax.all.*
import apps.WrapSever
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
      case WrapChoice.HWrap => WrapSever.severVertically(directives, width, height)
      case WrapChoice.VWrap => WrapSever.severHorizontally(directives, width, height)
      case WrapChoice.NoWrap =>
        val vertical = WrapSever.severVertically(directives, width, height)
        WrapSever.severHorizontally(vertical, width, height)
    result.pure[Sequencer].map(errorChannel.pure)
