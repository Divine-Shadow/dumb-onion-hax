package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.Path
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.nio.charset.StandardCharsets
import model.map.{MapNoHide, MapFileParser}

object MapProcessingServiceSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("process selects latest editor and writes transformed map") {
    val finder = new LatestEditorFinderImpl[IO]
    val copier = new MapEditorCopierImpl[IO]
    val writer = new MapWriterImpl[IO]
    val service = new MapProcessingServiceImpl[IO](finder, copier, writer)
    for
      rootDir <- IO(Files.createTempDirectory("root-editor"))
      older <- IO(Files.createDirectory(rootDir.resolve("older")))
      newer <- IO(Files.createDirectory(rootDir.resolve("newer")))
      _ <- IO(Files.write(older.resolve("old.map"), "#dom2title Old".getBytes(StandardCharsets.UTF_8)))
      _ <- IO(Files.setLastModifiedTime(older, FileTime.fromMillis(1000)))
      _ <- IO(Files.copy(Path("data/test-map.map").toNioPath, newer.resolve("map.map")))
      _ <- IO(Files.write(newer.resolve("image.tga"), Array[Byte](1,2,3)))
      _ <- IO(Files.setLastModifiedTime(newer, FileTime.fromMillis(2000)))
      destDir <- IO(Files.createTempDirectory("dest-editor"))
      resultEC <- service.process[EC](Path.fromNioPath(rootDir), Path.fromNioPath(destDir), ms => IO.pure(ms))
      outPath <- IO.fromEither(resultEC)
      directives <- MapFileParser.parseFile[IO](outPath).compile.toVector
    yield expect.all(
      outPath.fileName.toString == "map.map",
      directives.nonEmpty,
      !directives.contains(MapNoHide)
    )
  }
