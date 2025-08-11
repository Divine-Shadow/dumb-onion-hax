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
  NeighbourSpec,
  VWrapAround
}
import services.mapeditor.{
  GroundSurfaceDuelPipe,
  GroundSurfaceDuelPipeImpl,
  MapSizeValidatorImpl,
  PlacementPlannerImpl,
  GateDirectiveServiceImpl,
  ThronePlacementServiceImpl,
  WrapChoice,
  WrapChoiceService,
  WrapChoices
}
import services.mapeditor.WrapSeverService.isTopBottom
import weaver.SimpleIOSuite
import java.nio.file.{Files as JFiles, Path as JPath}
import java.nio.file.attribute.FileTime

object MapEditorWrapAppSpec extends SimpleIOSuite:
  override def maxParallelism = 1
  private class StubWrapChoiceService(selections: WrapChoices) extends WrapChoiceService[IO]:
    override def chooseWraps[ErrorChannel[_]]()(using
        errorChannel: MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]
    ) = IO.pure(errorChannel.pure(selections))

  private class StubGroundSurfaceDuelPipe extends GroundSurfaceDuelPipe[IO]:
    override def apply[ErrorChannel[_]](
        surface: fs2.Stream[IO, model.map.MapDirective],
        cave: fs2.Stream[IO, model.map.MapDirective],
        config: model.map.GroundSurfaceDuelConfig
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]) =
      (Vector.empty[model.map.MapDirective], Vector.empty[model.map.MapDirective]).pure[ErrorChannel].pure[IO]
  test("creates sample config when missing") {
    for
      configFile <- IO(JPath.of("map-editor-wrap.conf"))
      _ <- IO(JFiles.deleteIfExists(configFile))
      res <- MapEditorWrapApp
        .runWith(new StubWrapChoiceService(WrapChoices(WrapChoice.HWrap, None)), new StubGroundSurfaceDuelPipe)
        .attempt
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
      _ <- MapEditorWrapApp
        .runWith(new StubWrapChoiceService(WrapChoices(WrapChoice.HWrap, None)), new StubGroundSurfaceDuelPipe)
        .guarantee(IO(JFiles.deleteIfExists(configFile)))
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

  test("severs cave map when selected") {
    for
      rootDir <- IO(JFiles.createTempDirectory("root-editor-cave"))
      newer <- IO(JFiles.createDirectory(rootDir.resolve("newer")))
      _ <- IO(JFiles.copy(Path("data/five-by-twelve.map").toNioPath, newer.resolve("map.map")))
      _ <- IO(JFiles.copy(Path("data/five-by-twelve.map").toNioPath, newer.resolve("map_plane2.map")))
      destRoot <- IO(JFiles.createTempDirectory("dest-editor-cave"))
      configFile = JPath.of("map-editor-wrap.conf")
      _ <- IO(
        JFiles.writeString(
          configFile,
          s"""source="${rootDir.toString}"
dest="${destRoot.toString}"
"""
        )
      )
      _ <- MapEditorWrapApp
        .runWith(
          new StubWrapChoiceService(WrapChoices(WrapChoice.HWrap, Some(WrapChoice.VWrap))),
          new StubGroundSurfaceDuelPipe
        )
        .guarantee(IO(JFiles.deleteIfExists(configFile)))
      destDir = Path.fromNioPath(destRoot.resolve("newer"))
      cavePath = destDir / "map_plane2.map"
      directives <- MapFileParser.parseFile[IO](cavePath).compile.toVector
    yield expect(directives.contains(VWrapAround))
  }

  test("applies ground surface duel when selected") {
    val mapContent =
      """-- Map
#dom2title test
#imagefile test.tga
#mapsize 1280 800
#wraparound
#terrain 1 0
#terrain 5 0
#terrain 21 0
#terrain 25 0
""".stripMargin
    for
      rootDir <- IO(JFiles.createTempDirectory("root-editor-duel"))
      newer <- IO(JFiles.createDirectory(rootDir.resolve("newer")))
      _ <- IO(JFiles.writeString(newer.resolve("map.map"), mapContent))
      _ <- IO(JFiles.writeString(newer.resolve("map_plane2.map"), mapContent))
      destRoot <- IO(JFiles.createTempDirectory("dest-editor-duel"))
      configFile = JPath.of("map-editor-wrap.conf")
      _ <- IO(JFiles.writeString(configFile, s"""source="${rootDir.toString}"\ndest="${destRoot.toString}"\n"""))
      dueler = new GroundSurfaceDuelPipeImpl[IO](
        new MapSizeValidatorImpl[IO],
        new PlacementPlannerImpl[IO],
        new GateDirectiveServiceImpl[IO],
        new ThronePlacementServiceImpl[IO]
      )
      _ <- MapEditorWrapApp
        .runWith(new StubWrapChoiceService(WrapChoices(WrapChoice.GroundSurfaceDuel, None)), dueler)
        .guarantee(IO(JFiles.deleteIfExists(configFile)))
      destDir = Path.fromNioPath(destRoot.resolve("newer"))
      surfPath = destDir / "map.map"
      cavePath = destDir / "map_plane2.map"
      surfDirectives <- MapFileParser.parseFile[IO](surfPath).compile.toVector
      caveDirectives <- MapFileParser.parseFile[IO](cavePath).compile.toVector
    yield expect.all(
      surfDirectives.contains(model.map.NoWrapAround),
      caveDirectives.contains(model.map.NoWrapAround)
    )
  }
