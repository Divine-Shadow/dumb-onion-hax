package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.{MonadError, Traverse}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import cats.effect.Async

final case class CopiedMapStreams[Sequencer[_]](
    main: (Stream[Sequencer, Byte], Path),
    cave: Option[(Stream[Sequencer, Byte], Path)]
)

trait MapEditorCopier[Sequencer[_]]:
  def copyWithoutMaps[ErrorChannel[_]](
      source: Path,
      dest: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[CopiedMapStreams[Sequencer]]]

class MapEditorCopierImpl[Sequencer[_]: Async: Files] extends MapEditorCopier[Sequencer]:
  protected val sequencer = summon[Async[Sequencer]]

  override def copyWithoutMaps[ErrorChannel[_]](
      source: Path,
      dest: Path
  )(using files: Files[Sequencer],
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
  ): Sequencer[ErrorChannel[CopiedMapStreams[Sequencer]]] =
    for
      _ <- sequencer.delay(println(s"Copying editor from $source to $dest"))
      entries <- Files[Sequencer].walk(source).compile.toList
      info <- entries.traverse(p => Files[Sequencer].isDirectory(p).map(b => (p, b)))
      maybeMain =
        info.collectFirst {
          case (p, false)
              if {
                val lower = p.fileName.toString.toLowerCase
                lower.endsWith(".map") && !lower.endsWith("_plane2.map")
              } => p
        }
      maybeCave =
        info.collectFirst {
          case (p, false)
              if {
                val lower = p.fileName.toString.toLowerCase
                lower.endsWith("_plane2.map")
              } => p
        }
      _ <- info.traverse_ { case (p, isDir) =>
            val rel = source.relativize(p)
            val target = dest / rel.toString
            if isDir then Files[Sequencer].createDirectories(target)
            else if maybeMain.contains(p) || maybeCave.contains(p) then Async[Sequencer].unit
            else
              val parent = target.parent.getOrElse(dest)
              Files[Sequencer].createDirectories(parent) >>
                Files[Sequencer].deleteIfExists(target) >>
                Files[Sequencer].copy(p, target)
          }
      result <- maybeMain match
        case Some(mapPath) =>
          val main = (Files[Sequencer].readAll(mapPath), dest / mapPath.fileName)
          val cave = maybeCave.map(p => (Files[Sequencer].readAll(p), dest / p.fileName))
          CopiedMapStreams(main, cave).pure[ErrorChannel].pure[Sequencer]
        case None =>
          errorChannel
            .raiseError[CopiedMapStreams[Sequencer]](new NoSuchElementException(".map file not found"))
            .pure[Sequencer]
    yield result
