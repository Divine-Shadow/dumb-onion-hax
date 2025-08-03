package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import cats.effect.Async
import model.map.MapDirective

trait MapDirectiveTransformer[Sequencer[_]]:
  def transform[ErrorChannel[_]](
      directives: Stream[Sequencer, MapDirective],
      pipe: Pipe[Sequencer, MapDirective, MapDirective]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]]

class MapDirectiveTransformerImpl[Sequencer[_]: Async] extends MapDirectiveTransformer[Sequencer]:
  override def transform[ErrorChannel[_]](
      directives: Stream[Sequencer, MapDirective],
      pipe: Pipe[Sequencer, MapDirective, MapDirective]
  )(using errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Vector[MapDirective]]] =
    directives
      .through(pipe)
      .compile
      .toVector
      .map(_.pure[ErrorChannel])
