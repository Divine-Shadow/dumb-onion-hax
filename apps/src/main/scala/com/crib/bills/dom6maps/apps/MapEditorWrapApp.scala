package com.crib.bills.dom6maps
package apps

import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import model.map.{MapFileParser, MapSize}
import services.mapeditor.{MapEditorCopierImpl, MapWriterImpl}

object MapEditorWrapApp extends IOApp:
  private type ErrorOr[A] = Either[Throwable, A]
  override def run(args: List[String]): IO[ExitCode] =
    args match
      case source :: dest :: Nil =>
        val src = Path(source)
        val dst = Path(dest)
        val copier = new MapEditorCopierImpl[IO]
        val writer = new MapWriterImpl[IO]
        val action =
          for
            res <- copier.copyWithoutMap[ErrorOr](src, dst)
            (bytes, outPath) <- IO.fromEither(res)
            directives <- bytes.through(MapFileParser.parse[IO]).compile.toVector
            (w, h) <- IO.fromOption(directives.collectFirst { case MapSize(w, h) => (w, h) })(
              new NoSuchElementException("#mapsize not found")
            )
            severed = WrapSever.severVertically(directives, w, h)
            fileName = outPath.fileName.toString.stripSuffix(".map") + ".hwrap.map"
            target = dst / fileName
            written <- writer.write[ErrorOr](severed, target)
            _ <- IO.fromEither(written)
          yield ExitCode.Success
        action
      case _ =>
        IO.println("Usage: MapEditorWrapApp <input-dir> <output-dir>").as(ExitCode(2))
