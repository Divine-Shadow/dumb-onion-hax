package com.crib.bills.dom6maps
package apps.services.mapeditor

import cats.effect.IO
import cats.instances.either.*
import cats.syntax.all.*
import fs2.io.file.{Files as Fs2Files, Path}
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.charset.StandardCharsets

object MapEditorCopierSpec extends SimpleIOSuite:
  type EC[A] = Either[Throwable, A]

  test("copies assets while extracting map files") {
    val copier = new MapEditorCopierImpl[IO]
    for
      sourceDir <- IO(Files.createTempDirectory("source-editor"))
      destDir <- IO(Files.createTempDirectory("dest-editor"))
      _ <- IO(Files.write(sourceDir.resolve("map.map"), "#dom2title Test".getBytes(StandardCharsets.UTF_8)))
      _ <- IO(Files.write(sourceDir.resolve("map_plane2.map"), "#dom2title Cave".getBytes(StandardCharsets.UTF_8)))
      _ <- IO(Files.write(sourceDir.resolve("image.tga"), Array[Byte](1, 2, 3)))
      result <- copier.copyWithoutMaps[EC](Path.fromNioPath(sourceDir), Path.fromNioPath(destDir))
      streams <- IO.fromEither(result)
      (stream, outPath) = streams.main
      bytes <- stream.compile.toVector
      destEntries <- Fs2Files[IO].list(Path.fromNioPath(destDir)).compile.toList
      caveExists = streams.cave.isDefined
    yield expect.all(
      bytes == "#dom2title Test".getBytes(StandardCharsets.UTF_8).toVector,
      destEntries.exists(_.fileName.toString == "image.tga"),
      !destEntries.exists(_.fileName.toString == "map.map"),
      !destEntries.exists(_.fileName.toString == "map_plane2.map"),
      outPath.fileName.toString == "map.map",
      caveExists,
    )
  }
