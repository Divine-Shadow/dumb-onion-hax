package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.{Files => Fs2Files, Path}
import com.crib.bills.dom6maps.model.map.{
  HWrapAround,
  MapFileParser,
  MapSize,
  Neighbour,
  NeighbourSpec
}
import WrapSever.isTopBottom
import weaver.SimpleIOSuite
import java.nio.file.Files
import java.nio.file.attribute.FileTime

object MapEditorWrapAppSpec extends SimpleIOSuite:
  test("copies latest editor and severs map to hwrap") {
    for
      rootDir <- IO(Files.createTempDirectory("root-editor"))
      older <- IO(Files.createDirectory(rootDir.resolve("older")))
      newer <- IO(Files.createDirectory(rootDir.resolve("newer")))
      _ <- IO(Files.copy(Path("data/five-by-twelve.map").toNioPath, older.resolve("old.map")))
      _ <- IO(Files.write(older.resolve("old.tga"), Array[Byte](1,2,3)))
      _ <- IO(Files.setLastModifiedTime(older, FileTime.fromMillis(1000)))
      _ <- IO(Files.copy(Path("data/five-by-twelve.map").toNioPath, newer.resolve("map.map")))
      _ <- IO(Files.write(newer.resolve("image.tga"), Array[Byte](1,2,3)))
      _ <- IO(Files.setLastModifiedTime(newer, FileTime.fromMillis(2000)))
      destRoot <- IO(Files.createTempDirectory("dest-editor"))
      _ <- MapEditorWrapApp.run(List(rootDir.toString, destRoot.toString))
      destEntries <- Fs2Files[IO].list(Path.fromNioPath(destRoot)).compile.toList
      destDir = Path.fromNioPath(destRoot.resolve("newer"))
      copiedEntries <- Fs2Files[IO].list(destDir).compile.toList
      mapPath = destDir / "map.hwrap.map"
      directives <- MapFileParser.parseFile[IO](mapPath).compile.toVector
      size <- IO.fromOption(directives.collectFirst { case MapSize(w, h) => (w, h) })(
        new NoSuchElementException("#mapsize not found")
      )
      (w, h) = size
      hasTopBottom = directives.exists {
        case Neighbour(a, b)     => isTopBottom(a, b, w, h)
        case NeighbourSpec(a,b,_)=> isTopBottom(a, b, w, h)
        case _                   => false
      }
    yield expect.all(
      destEntries.exists(_.fileName.toString == "newer"),
      copiedEntries.exists(_.fileName.toString == "image.tga"),
      copiedEntries.exists(_.fileName.toString == "map.hwrap.map"),
      !copiedEntries.exists(_.fileName.toString == "map.map"),
      directives.contains(HWrapAround),
      !hasTopBottom
    )
  }
