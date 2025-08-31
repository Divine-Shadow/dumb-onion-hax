package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import cats.{MonadError, Traverse}
import fs2.io.file.Path
import model.map.{MapFileParser, MapState, FeatureId}
import services.mapeditor.{
  GroundSurfaceDuelPipe,
  ThronePlacementServiceImpl,
  MapLayerLoaderImpl,
  WrapChoice,
  WrapChoiceService,
  WrapChoices,
  MapEditorSettings,
  GroundSurfaceNationService
}
import weaver.SimpleIOSuite
import java.nio.file.{Files => JFiles, Path => JPath}

object FullWrapThronePlacementSpec extends SimpleIOSuite:
  override def maxParallelism = 1

  private class StubWrapChoiceService(selections: WrapChoices) extends WrapChoiceService[IO]:
    override def chooseSettings[ErrorChannel[_]](
        config: model.map.ThroneFeatureConfig
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]) =
      IO.pure(summon[MonadError[ErrorChannel, Throwable]].pure(MapEditorSettings(selections, config)))

  private class StubGroundSurfaceNationService extends GroundSurfaceNationService[IO]:
    override def chooseNations[ErrorChannel[_]]()(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]) =
      IO.pure(summon[MonadError[ErrorChannel, Throwable]].pure(model.map.DuelNations(model.map.SurfaceNation(model.Nation.Agartha_Early), model.map.UndergroundNation(model.Nation.Atlantis_Early))))

  private class StubGroundSurfaceDuelPipe extends GroundSurfaceDuelPipe[IO]:
    override def apply[ErrorChannel[_]](
        surface: model.map.MapState,
        cave: model.map.MapState,
        config: model.map.GroundSurfaceDuelConfig,
        surfaceNation: model.map.SurfaceNation,
        undergroundNation: model.map.UndergroundNation
    )(using MonadError[ErrorChannel, Throwable] & Traverse[ErrorChannel]) =
      (model.map.MapState.empty, model.map.MapState.empty).pure[ErrorChannel].pure[IO]

  test("places overridden thrones for full wrap") {
    val overrides =
      """overrides = [
        { x = 0, y = 0, level = 2 },
        { x = 6, y = 0, level = 2 },
        { x = 0, y = 6, level = 2 },
        { x = 6, y = 6, level = 2 },

        { x = 3, y = 0, level = 1 },
        { x = 9, y = 0, level = 1 },
        { x = 3, y = 6, level = 1 },
        { x = 9, y = 6, level = 1 },

        { x = 0, y = 3, level = 1 },
        { x = 6, y = 3, level = 1 },
        { x = 0, y = 9, level = 1 },
        { x = 6, y = 9, id = 1338 }
      ]
      """.stripMargin
    for
      rootDir <- IO(JFiles.createTempDirectory("wrap-src"))
      latest <- IO(JFiles.createDirectory(rootDir.resolve("latest")))
      _ <- IO(JFiles.copy(Path("data/eight-by-eight.map").toNioPath, latest.resolve("map.map")))
      _ <- IO(JFiles.write(latest.resolve("image.tga"), Array[Byte](1,2,3)))
      destRoot <- IO(JFiles.createTempDirectory("wrap-dest"))
      configFile = JPath.of("map-editor-wrap.conf")
      _ <- IO(
        JFiles.writeString(
          configFile,
          s"""source="${rootDir.toString}"
             |dest="${destRoot.toString}"
             |""".stripMargin
        )
      )
      overridesFile = JPath.of("throne-override.conf")
      _ <- IO(JFiles.writeString(overridesFile, overrides))
      _ <- MapEditorWrapApp
        .runWith(
          new MapLayerLoaderImpl[IO],
          new StubWrapChoiceService(WrapChoices(WrapChoice.FullWrap, None)),
          new StubGroundSurfaceNationService,
          new StubGroundSurfaceDuelPipe,
          new ThronePlacementServiceImpl[IO]
        )
        .guarantee(IO(JFiles.deleteIfExists(configFile)) *> IO(JFiles.deleteIfExists(overridesFile)))
      destDir = Path.fromNioPath(destRoot.resolve("latest"))
      mapPath = destDir / "map.map"
      directives <- MapFileParser.parseFile[IO](mapPath).compile.toVector
      state <- MapState.fromDirectives(fs2.Stream.emits(directives).covary[IO])
      features = state.features
    yield expect.all(features.length == 12, features.exists(_.id == FeatureId(1338)))
  }
