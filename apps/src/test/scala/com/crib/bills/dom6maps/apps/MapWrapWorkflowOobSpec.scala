package com.crib.bills.dom6maps
package apps

import cats.effect.IO
import cats.syntax.all.*
import cats.instances.either.*
import fs2.io.file.Files
import weaver.SimpleIOSuite
import java.nio.file.{Files as JFiles, Path as JPath}

import apps.services.mapeditor.*
import model.version.{UpdateStatus, Version}

object MapWrapWorkflowOobSpec extends SimpleIOSuite:
  override def maxParallelism = 1
  private type ErrorOr[A] = Either[Throwable, A]
  given Files[IO] = Files.forAsync[IO]

  private class StubReleaseChecker extends apps.services.update.Service[IO]:
    override def checkForUpdate[ErrorChannel[_]](current: Version)(using cats.MonadError[ErrorChannel, Throwable] & cats.Traverse[ErrorChannel]) =
      IO.pure(summon[cats.MonadError[ErrorChannel, Throwable]].pure(UpdateStatus.CurrentVersionIsLatest))

  private class StubChooser extends WrapChoiceService[IO]:
    override def chooseSettings[ErrorChannel[_]](
        config: model.map.ThroneFeatureConfig,
        caveAvailability: CaveLayerAvailability
    )(using cats.MonadError[ErrorChannel, Throwable] & cats.Traverse[ErrorChannel]) =
      IO.pure(
        summon[cats.MonadError[ErrorChannel, Throwable]]
          .pure(MapEditorSettings(WrapChoices(WrapChoice.FullWrap, None), config, MagicSiteSelection.Disabled))
      )

  private class StubNations extends GroundSurfaceNationService[IO]:
    override def chooseNations[ErrorChannel[_]]()(using cats.MonadError[ErrorChannel, Throwable] & cats.Traverse[ErrorChannel]) =
      IO.pure(summon[cats.MonadError[ErrorChannel, Throwable]].pure(model.map.DuelNations(model.map.SurfaceNation(model.Nation.Agartha_Early), model.map.UndergroundNation(model.Nation.Atlantis_Early))))

  private class StubDuel extends GroundSurfaceDuelPipe[IO]:
    override def apply[ErrorChannel[_]](surface: model.map.MapState, cave: model.map.MapState, config: model.map.GroundSurfaceDuelConfig, surfaceNation: model.map.SurfaceNation, undergroundNation: model.map.UndergroundNation)(using cats.MonadError[ErrorChannel, Throwable] & cats.Traverse[ErrorChannel]) =
      (surface, cave).pure[ErrorChannel].pure[IO]

  test("MapWrapWorkflow returns Left when overrides are out-of-bounds") {
    val finder    = new LatestEditorFinderImpl[IO]
    val copier    = new MapEditorCopierImpl[IO]
    val writer    = new MapWriterImpl[IO]
    val loader    = new MapLayerLoaderImpl[IO]
    val converter = new WrapConversionServiceImpl[IO]
    val checker   = new StubReleaseChecker
    val chooser   = new StubChooser
    val nations   = new StubNations
    val dueler    = new StubDuel
    val thrones   = new ThronePlacementServiceImpl[IO]
    val magicSites = new MagicSiteFlagServiceImpl
    val workflow  = new MapWrapWorkflowImpl(finder, copier, writer, loader, converter, checker, chooser, nations, dueler, thrones, magicSites, Version("test"))

    val overrides = """overrides = [ { x = 9, y = 0, level = 1 } ]"""

    for
      srcRoot <- IO(JFiles.createTempDirectory("oob-src"))
      latest  <- IO(JFiles.createDirectory(srcRoot.resolve("latest")))
      _       <- IO(JFiles.copy(fs2.io.file.Path("data/eight-by-eight.map").toNioPath, latest.resolve("map.map")))
      _       <- IO(JFiles.write(latest.resolve("image.tga"), Array[Byte](1,2,3)))
      destRoot <- IO(JFiles.createTempDirectory("oob-dest"))
      cfg = PathsConfig(srcRoot, destRoot)
      // Point overrides to a temp file with OOB coordinates
      overridesFile <- IO(JFiles.createTempFile("throne-oob", ".conf"))
      _ <- IO(JFiles.writeString(overridesFile, overrides))
      _ <- IO(sys.props.update("dom6.overridesPath", overridesFile.toString))
      _ <- IO(sys.props.update("dom6.ignoreOverrides", "false"))
      res <- workflow.run(cfg)
      _ <- IO(sys.props.remove("dom6.overridesPath")).void
      _ <- IO(sys.props.remove("dom6.ignoreOverrides")).void
    yield expect(res.isLeft)
  }
