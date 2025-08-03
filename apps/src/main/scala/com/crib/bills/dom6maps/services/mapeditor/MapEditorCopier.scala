package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async

trait MapEditorCopier[Sequencer[_]]:
  def copyWithoutMap[ErrorChannel[_]](
      source: Path,
      dest: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[(Stream[Sequencer, Byte], Path)]]

class MapEditorCopierImpl[Sequencer[_]: Async: Files] extends MapEditorCopier[Sequencer]:
  override def copyWithoutMap[ErrorChannel[_]](
      source: Path,
      dest: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[(Stream[Sequencer, Byte], Path)]] =
    for
      entries <- Files[Sequencer].walk(source).compile.toList
      info <- entries.traverse(p => Files[Sequencer].isDirectory(p).map(b => (p, b)))
      maybeMap = info.collectFirst { case (p, false) if p.toString.endsWith(".map") => p }
      _ <- info.traverse_ { case (p, isDir) =>
            val rel = source.relativize(p)
            val target = dest / rel.toString
            if isDir then Files[Sequencer].createDirectories(target)
            else if maybeMap.contains(p) then Async[Sequencer].unit
            else Files[Sequencer].createDirectories(target.parent.getOrElse(dest)) >> Files[Sequencer].copy(p, target)
          }
      result <- maybeMap match
        case Some(mapPath) =>
          ((Files[Sequencer].readAll(mapPath), dest / mapPath.fileName)).pure[ErrorChannel].pure[Sequencer]
        case None =>
          errorChannel
            .raiseError[(Stream[Sequencer, Byte], Path)](new NoSuchElementException(".map file not found"))
            .pure[Sequencer]
    yield result
