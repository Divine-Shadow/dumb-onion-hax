package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import cats.{MonadError, Traverse}
import fs2.io.file.{Files as Fs2Files, Path}
import com.crib.bills.dom6maps.model.map.{
  HWrapAround,
  MapFileParser,
  MapSizePixels,
  Neighbour,
  NeighbourSpec
}
import services.mapeditor.{WrapChoice, WrapChoiceService}
import WrapSever.isTopBottom
import weaver.SimpleIOSuite
import java.nio.file.{Files as JFiles, Path as JPath}
import java.nio.file.attribute.FileTime

object MapEditorWrapAppSpec extends SimpleIOSuite:
  override def maxParallelism = 1
  private class StubWrapChoiceService(choice: WrapChoice) extends WrapChoiceService[IO]:
    override def chooseWrap[ErrorChannel[_]]()(using
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ) = IO.pure(errorChannel.pure(choice))
  test("creates sample config when missing") {
    for
      configFile <- IO(JPath.of("map-editor-wrap.conf"))
      _ <- IO(JFiles.deleteIfExists(configFile))
      res <- MapEditorWrapApp.runWith(new StubWrapChoiceService(WrapChoice.HWrap)).attempt
      exists <- IO(JFiles.exists(configFile))
      content <- IO(if exists then JFiles.readString(configFile) else "")
      _ <- IO(JFiles.deleteIfExists(configFile))
    yield expect.all(res.isLeft, exists, content.contains("source="), content.contains("dest="))
  }

  test("copies latest editor and severs map to hwrap") {
    for
      rootDir <- IO(JFiles.createTempDirectory("root-editor"))
      older <- IO(JFiles.createDirectory(rootDir.resolve("older")))
      newer <- IO(JFiles.createDirectory(rootDir.resolve("newer")))
      _ <- IO(JFiles.copy(Path("data/five-by-twelve.map").toNioPath, older.resolve("old.map")))
      _ <- IO(JFiles.write(older.resolve("old.tga"), Array[Byte](1, 2, 3)))
      _ <- IO(JFiles.setLastModifiedTime(older, FileTime.fromMillis(1000)))
      _ <- IO(JFiles.copy(Path("data/five-by-twelve.map").toNioPath, newer.resolve("map.map")))
      _ <- IO(JFiles.write(newer.resolve("image.tga"), Array[Byte](1, 2, 3)))
      _ <- IO(JFiles.setLastModifiedTime(newer, FileTime.fromMillis(2000)))
      destRoot <- IO(JFiles.createTempDirectory("dest-editor"))
      configFile = JPath.of("map-editor-wrap.conf")
      _ <- IO(JFiles.writeString(configFile, s"""source="${rootDir.toString}"
dest="${destRoot.toString}"
"""))
      _ <- MapEditorWrapApp.runWith(new StubWrapChoiceService(WrapChoice.HWrap)).guarantee(IO(JFiles.deleteIfExists(configFile)))
      destEntries <- Fs2Files[IO].list(Path.fromNioPath(destRoot)).compile.toList
      destDir = Path.fromNioPath(destRoot.resolve("newer"))
      copiedEntries <- Fs2Files[IO].list(destDir).compile.toList
      mapPath = destDir / "map.map"
      directives <- MapFileParser.parseFile[IO](mapPath).compile.toVector
      sizePixels <- IO.fromOption(directives.collectFirst { case m: MapSizePixels => m })(
        new NoSuchElementException("#mapsize not found")
      )
      provinceSize = sizePixels.toProvinceSize
      w = provinceSize.width
      h = provinceSize.height
      hasTopBottom = directives.exists {
        case Neighbour(a, b)       => isTopBottom(a, b, w, h)
        case NeighbourSpec(a, b, _) => isTopBottom(a, b, w, h)
        case _                     => false
      }
    yield expect.all(
      destEntries.exists(_.fileName.toString == "newer"),
      copiedEntries.exists(_.fileName.toString == "image.tga"),
      copiedEntries.exists(_.fileName.toString == "map.map"),
      !copiedEntries.exists(_.fileName.toString == "map.hwrap.map"),
      directives.contains(HWrapAround),
      !hasTopBottom,
    )
  }
