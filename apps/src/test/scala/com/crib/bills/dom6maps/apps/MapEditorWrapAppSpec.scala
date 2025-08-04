package com.crib.bills.dom6maps
package apps

import cats.effect.IO
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

object MapEditorWrapAppSpec extends SimpleIOSuite:
  test("copies editor and severs map to hwrap") {
    for
      srcDir <- IO(Files.createTempDirectory("source-editor"))
      _ <- IO(Files.copy(Path("data/five-by-twelve.map").toNioPath, srcDir.resolve("map.map")))
      _ <- IO(Files.write(srcDir.resolve("image.tga"), Array[Byte](1,2,3)))
      destDir <- IO(Files.createTempDirectory("dest-editor"))
      _ <- MapEditorWrapApp.run(List(srcDir.toString, destDir.toString))
      destEntries <- Fs2Files[IO].list(Path.fromNioPath(destDir)).compile.toList
      mapPath = Path.fromNioPath(destDir.resolve("map.hwrap.map"))
      directives <- MapFileParser.parseFile[IO](mapPath).compile.toVector
      size = directives.collectFirst { case MapSize(w, h) => (w, h) }.get
      (w, h) = size
      hasTopBottom = directives.exists {
        case Neighbour(a, b)     => isTopBottom(a, b, w, h)
        case NeighbourSpec(a,b,_)=> isTopBottom(a, b, w, h)
        case _                   => false
      }
    yield expect.all(
      destEntries.exists(_.fileName.toString == "image.tga"),
      destEntries.exists(_.fileName.toString == "map.hwrap.map"),
      !destEntries.exists(_.fileName.toString == "map.map"),
      directives.contains(HWrapAround),
      !hasTopBottom
    )
  }
