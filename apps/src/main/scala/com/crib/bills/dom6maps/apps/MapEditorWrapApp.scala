package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import services.mapeditor.{MapEditorCopierImpl, MapWriterImpl}
import model.map.{MapFileParser, MapSize}

object MapEditorWrapApp extends IOApp:
  type EC[A] = Either[Throwable, A]

  override def run(args: List[String]): IO[ExitCode] =
    args match
      case source :: dest :: Nil =>
        val src = Path(source)
        val dst = Path(dest)
        val copier = new MapEditorCopierImpl[IO]
        val writer = new MapWriterImpl[IO]
        val process: IO[EC[Unit]] =
          copier.copyWithoutMap[EC](src, dst).flatMap {
            case Left(err) => IO.pure(Left(err))
            case Right((bytes, outPath)) =>
              bytes.through(MapFileParser.parse[IO]).compile.toVector.flatMap { directives =>
                directives.collectFirst { case MapSize(w, h) => (w, h) } match
                  case Some((w, h)) =>
                    val severed = WrapSever.severVertically(directives, w, h)
                    val fileName = outPath.fileName.toString.stripSuffix(".map") + ".hwrap.map"
                    val target = dst / fileName
                    writer.write[EC](severed, target)
                  case None =>
                    IO.pure(Left(new NoSuchElementException("#mapsize not found")))
              }
          }
        process.flatMap {
          case Left(err) => IO.raiseError(err)
          case Right(_)  => IO.pure(ExitCode.Success)
        }
      case _ =>
        IO.println("Usage: MapEditorWrapApp <input-dir> <output-dir>").as(ExitCode(2))
