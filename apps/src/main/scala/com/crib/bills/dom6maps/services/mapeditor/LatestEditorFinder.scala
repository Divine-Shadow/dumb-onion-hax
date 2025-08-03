package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import cats.effect.Async

trait LatestEditorFinder[Sequencer[_]]:
  def mostRecentFolder[ErrorChannel[_]](
      root: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]]

class LatestEditorFinderImpl[Sequencer[_]: Async: Files] extends LatestEditorFinder[Sequencer]:
  override def mostRecentFolder[ErrorChannel[_]](
      root: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[Path]] =
    Files[Sequencer]
      .list(root)
      .evalFilter(p => Files[Sequencer].isDirectory(p))
      .evalMap(p => Files[Sequencer].getLastModifiedTime(p).map(time => (p, time)))
      .compile
      .toList
      .flatMap { list =>
        list.sortBy(_._2.toMillis).lastOption match
          case Some((p, _)) => p.pure[ErrorChannel].pure[Sequencer]
          case None =>
            errorChannel
              .raiseError[Path](new NoSuchElementException("no directories found"))
              .pure[Sequencer]
      }
